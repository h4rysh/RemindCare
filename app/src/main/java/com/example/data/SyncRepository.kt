package com.example.data

import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    // Pairing Flow
    suspend fun generatePairingCode(patientName: String): String
    fun listenForPairingRequest(code: String): Flow<PairingRequest?>
    suspend fun approvePairingRequest(code: String, caregiverId: String, caregiverName: String): Boolean
    suspend fun declinePairingRequest(code: String): Boolean

    fun requestPairing(code: String, caregiverId: String, caregiverName: String): Flow<SyncResult>
    suspend fun disconnect(patientId: String, caregiverId: String)
    
    // Sync Flow
    suspend fun syncRemindersToCloud(patientId: String, reminders: List<Reminder>)
    suspend fun deleteCloudReminder(patientId: String, reminderId: Long)
    fun listenForCloudReminders(patientId: String): Flow<List<Reminder>>
    
    suspend fun syncHistoryToCloud(patientId: String, history: List<ReminderHistory>)
    fun listenForCloudHistory(patientId: String): Flow<List<ReminderHistory>>
    
    // Auth & Identity
    fun getDeviceId(): String
}

data class PairingRequest(
    val caregiverId: String,
    val caregiverName: String,
    val patientId: String = "",
    val patientName: String = "",
    val status: String // "pending", "requested", "approved", "declined"
)

sealed class SyncResult {
    data class Success(val patientId: String, val patientName: String) : SyncResult()
    object PendingApproval : SyncResult()
    data class Error(val message: String) : SyncResult()
}
