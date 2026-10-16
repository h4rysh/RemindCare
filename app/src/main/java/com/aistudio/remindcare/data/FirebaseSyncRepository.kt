package com.aistudio.remindcare.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseSyncRepository(private val context: Context) : SyncRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private suspend fun ensureAuth(): String {
        val current = auth.currentUser
        if (current != null) return current.uid
        return try {
            auth.signInAnonymously().await().user?.uid ?: getDeviceId()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Failed to sign in anonymously", e)
            com.aistudio.remindcare.util.CrashlyticsManager.recordNonFatal(e, "Anonymous sign in failed")
            getDeviceId()
        }
    }

    override fun getDeviceId(): String {
        return auth.currentUser?.uid ?: run {
            val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            var id = prefs.getString("device_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("device_id", id).apply()
            }
            id
        }
    }

    override suspend fun generatePairingCode(patientName: String): String {
        val deviceId = ensureAuth()
        val code = (100000..999999).random().toString()
        val data = hashMapOf(
            "code" to code,
            "patientId" to deviceId,
            "patientName" to patientName,
            "status" to PairingStatus.PENDING.name,
            "caregiverId" to "",
            "caregiverName" to "",
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        try {
            kotlinx.coroutines.withTimeout(15000L) {
                firestore.collection("pairing_requests").document(code).set(data).await()
            }
            com.aistudio.remindcare.util.CrashlyticsManager.log("FirebaseSync", "Pairing code created successfully")
        } catch (e: Exception) {
            com.aistudio.remindcare.util.CrashlyticsManager.recordNonFatal(e, "generatePairingCode failed")
            throw e
        }
        return code
    }

    override fun listenForPairingRequest(code: String): Flow<PairingRequest?> = callbackFlow {
        val subscription = firestore.collection("pairing_requests").document(code)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseSync", "Listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val statusStr = snapshot.getString("status") ?: ""
                    val caregiverId = snapshot.getString("caregiverId") ?: ""
                    val caregiverName = snapshot.getString("caregiverName") ?: ""
                    
                    if (statusStr == PairingStatus.REQUESTED.name) {
                        trySend(PairingRequest(caregiverId = caregiverId, caregiverName = caregiverName, status = PairingStatus.REQUESTED))
                    } else {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun approvePairingRequest(code: String, caregiverId: String, caregiverName: String): Boolean {
        return try {
            val patientId = ensureAuth()
            firestore.collection("pairing_requests").document(code)
                .update("status", PairingStatus.APPROVED.name, "authorizedCaregiverId", caregiverId).await()
            
            // Record authorization mapping for security rules and tenant isolation
            firestore.collection("caregiver_authorizations").document(patientId).set(
                mapOf(
                    "patientId" to patientId,
                    "caregiverId" to caregiverId,
                    "approvedAt" to com.google.firebase.Timestamp.now()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Failed to approve pairing request", e)
            com.aistudio.remindcare.util.CrashlyticsManager.recordNonFatal(e, "approvePairingRequest failed")
            false
        }
    }

    override suspend fun declinePairingRequest(code: String): Boolean {
        return try {
            firestore.collection("pairing_requests").document(code)
                .update("status", PairingStatus.DECLINED.name).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun requestPairing(code: String, caregiverId: String, caregiverName: String): Flow<SyncResult> = callbackFlow {
        trySend(SyncResult.PendingApproval)
        
        try {
            val currentCaregiverId = ensureAuth()
            val doc = firestore.collection("pairing_requests").document(code).get().await()
            if (doc.exists()) {
                val statusStr = doc.getString("status")
                if (statusStr == PairingStatus.PENDING.name) {
                    firestore.collection("pairing_requests").document(code).update(
                        mapOf(
                            "status" to PairingStatus.REQUESTED.name,
                            "caregiverId" to currentCaregiverId,
                            "caregiverName" to caregiverName
                        )
                    ).await()
                    
                    // Listen for approval
                    val subscription = firestore.collection("pairing_requests").document(code)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("FirebaseSync", "Listen failed.", error)
                                com.aistudio.remindcare.util.CrashlyticsManager.recordNonFatal(error, "pairing_requests snapshot listener error")
                                trySend(SyncResult.Error(error.message ?: "Listen error"))
                                close(error)
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                val currentStatus = snapshot.getString("status")
                                val patientName = snapshot.getString("patientName") ?: "Patient"
                                val patientId = snapshot.getString("patientId") ?: ""
                                if (currentStatus == PairingStatus.APPROVED.name) {
                                    trySend(SyncResult.Success(patientId = patientId, patientName = patientName))
                                } else if (currentStatus == PairingStatus.DECLINED.name) {
                                    trySend(SyncResult.Error("Patient declined the connection."))
                                }
                            }
                        }
                    awaitClose { subscription.remove() }
                } else {
                    trySend(SyncResult.Error("Pairing code is no longer pending."))
                    close()
                }
            } else {
                trySend(SyncResult.Error("Invalid pairing code."))
                close()
            }
        } catch (e: Exception) {
            com.aistudio.remindcare.util.CrashlyticsManager.recordNonFatal(e, "requestPairing failed")
            trySend(SyncResult.Error(e.message ?: "Network error occurred."))
            close()
        }
    }

    override suspend fun disconnect(patientId: String, caregiverId: String) {
        try {
            if (patientId.isNotBlank()) {
                firestore.collection("caregiver_authorizations").document(patientId).delete().await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Error deleting authorization mapping on disconnect", e)
        }
    }

    override suspend fun syncRemindersToCloud(patientId: String, reminders: List<Reminder>) {
        if (patientId.isBlank()) return
        ensureAuth()
        val batch = firestore.batch()
        val collection = firestore.collection("patients").document(patientId).collection("reminders")
        
        for (r in reminders) {
            val docId = if (r.uuid.isNotBlank()) r.uuid else r.id.toString()
            val docRef = collection.document(docId)
            batch.set(docRef, r, com.google.firebase.firestore.SetOptions.merge())
        }
        batch.commit().await()
    }

    override suspend fun deleteCloudReminder(patientId: String, reminderId: Long) {
        val collection = firestore.collection("patients").document(patientId).collection("reminders")
        val docs = collection.whereEqualTo("id", reminderId).get().await()
        for (doc in docs) {
            doc.reference.delete().await()
        }
    }

    override fun listenForCloudReminders(patientId: String): Flow<List<Reminder>> = callbackFlow {
        val subscription = firestore.collection("patients").document(patientId).collection("reminders")
        val listener = subscription.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseSync", "Listen failed.", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                if (snapshot.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }
                val reminders = snapshot.toObjects(Reminder::class.java).filter { !it.isDeleted }
                trySend(reminders)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun syncHistoryToCloud(patientId: String, history: List<ReminderHistory>) {
        if (patientId.isBlank()) return
        ensureAuth()
        val batch = firestore.batch()
        val collection = firestore.collection("patients").document(patientId).collection("history")
        
        for (h in history) {
            val docId = if (h.uuid.isNotBlank()) h.uuid else h.id.toString()
            val docRef = collection.document(docId)
            batch.set(docRef, h, com.google.firebase.firestore.SetOptions.merge())
        }
        batch.commit().await()
    }

    override fun listenForCloudHistory(patientId: String): Flow<List<ReminderHistory>> = callbackFlow {
        val subscription = firestore.collection("patients").document(patientId).collection("history")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseSync", "Listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val history = snapshot.toObjects(ReminderHistory::class.java)
                    trySend(history)
                }
            }
        awaitClose { subscription.remove() }
    }
}
