package com.aistudio.remindcare.domain.model

enum class PairingStatus {
    PENDING,
    REQUESTED,
    APPROVED,
    DECLINED
}

data class PairingRequest(
    val caregiverId: String,
    val caregiverName: String,
    val status: PairingStatus
)

sealed class SyncResult {
    data class Success(val patientId: String, val patientName: String) : SyncResult()
    object PendingApproval : SyncResult()
    data class Error(val message: String) : SyncResult()
}
