package com.aistudio.remindcare.data.repository

import com.aistudio.remindcare.data.SyncRepository
import com.aistudio.remindcare.data.mapper.PairingMapper
import com.aistudio.remindcare.domain.model.PairingRequest
import com.aistudio.remindcare.domain.model.SyncResult
import com.aistudio.remindcare.domain.repository.PairingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PairingRepositoryImpl(
    private val syncRepository: SyncRepository
) : PairingRepository {

    override fun getDeviceId(): String {
        return syncRepository.getDeviceId()
    }

    override suspend fun generatePairingCode(patientName: String): String {
        return syncRepository.generatePairingCode(patientName)
    }

    override fun listenForPairingRequest(code: String): Flow<PairingRequest?> {
        return syncRepository.listenForPairingRequest(code).map { req ->
            req?.let { PairingMapper.toDomain(it) }
        }
    }

    override fun requestPairing(code: String, caregiverId: String, caregiverName: String): Flow<SyncResult> {
        return syncRepository.requestPairing(code, caregiverId, caregiverName).map { result ->
            PairingMapper.toDomain(result)
        }
    }

    override suspend fun approvePairingRequest(code: String, caregiverId: String, caregiverName: String): Boolean {
        return syncRepository.approvePairingRequest(code, caregiverId, caregiverName)
    }

    override suspend fun declinePairingRequest(code: String): Boolean {
        return syncRepository.declinePairingRequest(code)
    }

    override suspend fun disconnect(patientId: String, caregiverId: String) {
        syncRepository.disconnect(patientId, caregiverId)
    }
}
