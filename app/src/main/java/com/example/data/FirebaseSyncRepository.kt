package com.example.data

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseSyncRepository(private val context: Context) : SyncRepository {

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    private val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()

    override fun getDeviceId(): String = deviceId

    override suspend fun generatePairingCode(patientName: String): String {
        val code = "RC-" + (1000..9999).random().toString()
        val data = hashMapOf(
            "code" to code,
            "patientId" to deviceId,
            "patientName" to patientName,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )
        db?.collection("pairing_codes")?.document(code)?.set(data)?.await()
        return code
    }

    override fun listenForPairingRequest(code: String): Flow<PairingRequest?> = callbackFlow {
        val listener = db?.collection("pairing_codes")?.document(code)?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getString("status") ?: "pending"
                val caregiverId = snapshot.getString("caregiverId") ?: ""
                val caregiverName = snapshot.getString("caregiverName") ?: ""
                val patientId = snapshot.getString("patientId") ?: ""
                val patientName = snapshot.getString("patientName") ?: ""
                trySend(PairingRequest(caregiverId, caregiverName, patientId, patientName, status))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener?.remove() }
    }

    override suspend fun approvePairingRequest(code: String, caregiverId: String, caregiverName: String): Boolean {
        return try {
            db?.collection("pairing_codes")?.document(code)?.update("status", "approved")?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun declinePairingRequest(code: String): Boolean {
        return try {
            db?.collection("pairing_codes")?.document(code)?.update("status", "declined")?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun requestPairing(code: String, caregiverId: String, caregiverName: String): Flow<SyncResult> = callbackFlow {
        // Update document to requested
        try {
            db?.collection("pairing_codes")?.document(code)?.update(
                mapOf(
                    "status" to "requested",
                    "caregiverId" to caregiverId,
                    "caregiverName" to caregiverName
                )
            )?.await()
        } catch (e: Exception) {
            trySend(SyncResult.Error("Invalid or expired code."))
            close()
            return@callbackFlow
        }
        
        val listener = db?.collection("pairing_codes")?.document(code)?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(SyncResult.Error(error.message ?: "Unknown error"))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getString("status")
                when (status) {
                    "requested" -> trySend(SyncResult.PendingApproval)
                    "approved" -> {
                        val patientId = snapshot.getString("patientId") ?: ""
                        val patientName = snapshot.getString("patientName") ?: "Patient"
                        trySend(SyncResult.Success(patientId, patientName))
                        // Clean up
                        db?.collection("pairing_codes")?.document(code)?.delete()
                    }
                    "declined" -> {
                        trySend(SyncResult.Error("Connection request declined by patient."))
                        db?.collection("pairing_codes")?.document(code)?.delete()
                    }
                }
            }
        }
        listener?.let { awaitClose { it.remove() } } ?: close()
    }

    override suspend fun disconnect(patientId: String, caregiverId: String) {
        // Not heavily relying on a connection doc for now, just clearing local state is enough,
        // but if we had a connection doc we could delete it.
    }

    override suspend fun syncRemindersToCloud(patientId: String, reminders: List<Reminder>) {
        if (patientId.isBlank()) return
        for (reminder in reminders) {
            val docId = reminder.id.toString()
            val data = hashMapOf(
                "title" to reminder.title,
                "type" to reminder.type,
                "time" to reminder.time,
                "repeat" to reminder.repeat,
                "medicineName" to reminder.medicineName,
                "medicineQuantity" to reminder.medicineQuantity,
                "medicineLocation" to reminder.medicineLocation,
                "photoRequired" to reminder.photoRequired,
                "additionalNotes" to reminder.additionalNotes,
                "isActive" to reminder.isActive,
                "soundUri" to reminder.soundUri,
                "updatedAt" to System.currentTimeMillis()
            )
            db?.collection("patients")?.document(patientId)?.collection("reminders")?.document(docId)?.set(data, SetOptions.merge())?.await()
        }
    }

    override suspend fun deleteCloudReminder(patientId: String, reminderId: Long) {
        if (patientId.isBlank()) return
        try {
            db?.collection("patients")?.document(patientId)?.collection("reminders")?.document(reminderId.toString())?.delete()?.await()
        } catch (e: Exception) {}
    }

    override fun listenForCloudReminders(patientId: String): Flow<List<Reminder>> = callbackFlow {
        if (patientId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db?.collection("patients")?.document(patientId)?.collection("reminders")?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Reminder(
                            id = doc.id.toLongOrNull() ?: 0L,
                            title = doc.getString("title") ?: "",
                            type = doc.getString("type") ?: "General",
                            time = doc.getString("time") ?: "",
                            repeat = doc.getString("repeat") ?: "Once",
                            medicineName = doc.getString("medicineName"),
                            medicineQuantity = doc.getString("medicineQuantity"),
                            medicineLocation = doc.getString("medicineLocation"),
                            photoRequired = doc.getBoolean("photoRequired") ?: false,
                            additionalNotes = doc.getString("additionalNotes"),
                            isActive = doc.getBoolean("isActive") ?: true,
                            soundUri = doc.getString("soundUri")
                        )
                    } catch (e: Exception) { null }
                }
                trySend(list)
            }
        }
        awaitClose { listener?.remove() }
    }

    override suspend fun syncHistoryToCloud(patientId: String, history: List<ReminderHistory>) {
        if (patientId.isBlank()) return
        for (item in history) {
            val docId = item.id.toString()
            val data = hashMapOf(
                "reminderId" to item.reminderId,
                "title" to item.title,
                "type" to item.type,
                "scheduledTime" to item.scheduledTime,
                "completedTime" to item.completedTime,
                "status" to item.status,
                "notes" to item.notes,
                "updatedAt" to System.currentTimeMillis()
            )
            db?.collection("patients")?.document(patientId)?.collection("history")?.document(docId)?.set(data, SetOptions.merge())?.await()
        }
    }

    override fun listenForCloudHistory(patientId: String): Flow<List<ReminderHistory>> = callbackFlow {
        if (patientId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db?.collection("patients")?.document(patientId)?.collection("history")?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        ReminderHistory(
                            id = doc.id.toLongOrNull() ?: 0L,
                            reminderId = doc.getLong("reminderId") ?: 0L,
                            title = doc.getString("title") ?: "",
                            type = doc.getString("type") ?: "",
                            scheduledTime = doc.getString("scheduledTime") ?: "",
                            completedTime = doc.getString("completedTime"),
                            status = doc.getString("status") ?: "",
                            notes = doc.getString("notes")
                        )
                    } catch (e: Exception) { null }
                }
                trySend(list)
            }
        }
        awaitClose { listener?.remove() }
    }
}
