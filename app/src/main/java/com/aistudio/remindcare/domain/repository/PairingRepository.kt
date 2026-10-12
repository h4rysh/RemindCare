package com.aistudio.remindcare.domain.repository

import com.aistudio.remindcare.domain.model.PairingRequest
import com.aistudio.remindcare.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for Patient-Caregiver Pairing & Handshake.
 * Pure Kotlin contract.
 */
interface PairingRepository {
    fun getDeviceId(): String
    suspend fun generatePairingCode(patientName: String): String
    fun listenForPairingRequest(code: String): Flow<PairingRequest?>
    fun requestPairing(code: String, caregiverId: String, caregiverName: String): Flow<SyncResult>
    suspend fun approvePairingRequest(code: String, caregiverId: String, caregiverName: String): Boolean
    suspend fun declinePairingRequest(code: String): Boolean
    suspend fun disconnect(patientId: String, caregiverId: String)
}
