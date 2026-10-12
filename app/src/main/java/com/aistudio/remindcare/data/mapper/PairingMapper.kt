package com.aistudio.remindcare.data.mapper

import com.aistudio.remindcare.data.PairingRequest as DataPairingRequest
import com.aistudio.remindcare.data.PairingStatus as DataPairingStatus
import com.aistudio.remindcare.data.SyncResult as DataSyncResult
import com.aistudio.remindcare.domain.model.PairingRequest as DomainPairingRequest
import com.aistudio.remindcare.domain.model.PairingStatus as DomainPairingStatus
import com.aistudio.remindcare.domain.model.SyncResult as DomainSyncResult

object PairingMapper {
    fun toDomain(status: DataPairingStatus): DomainPairingStatus {
        return when (status) {
            DataPairingStatus.PENDING -> DomainPairingStatus.PENDING
            DataPairingStatus.REQUESTED -> DomainPairingStatus.REQUESTED
            DataPairingStatus.APPROVED -> DomainPairingStatus.APPROVED
            DataPairingStatus.DECLINED -> DomainPairingStatus.DECLINED
        }
    }

    fun toData(status: DomainPairingStatus): DataPairingStatus {
        return when (status) {
            DomainPairingStatus.PENDING -> DataPairingStatus.PENDING
            DomainPairingStatus.REQUESTED -> DataPairingStatus.REQUESTED
            DomainPairingStatus.APPROVED -> DataPairingStatus.APPROVED
            DomainPairingStatus.DECLINED -> DataPairingStatus.DECLINED
        }
    }

    fun toDomain(request: DataPairingRequest): DomainPairingRequest {
        return DomainPairingRequest(
            caregiverId = request.caregiverId,
            caregiverName = request.caregiverName,
            status = toDomain(request.status)
        )
    }

    fun toDomain(result: DataSyncResult): DomainSyncResult {
        return when (result) {
            is DataSyncResult.Success -> DomainSyncResult.Success(result.patientId, result.patientName)
            is DataSyncResult.PendingApproval -> DomainSyncResult.PendingApproval
            is DataSyncResult.Error -> DomainSyncResult.Error(result.message)
        }
    }

    fun toData(request: DomainPairingRequest): DataPairingRequest {
        return DataPairingRequest(
            caregiverId = request.caregiverId,
            caregiverName = request.caregiverName,
            status = toData(request.status)
        )
    }

    fun toData(result: DomainSyncResult): DataSyncResult {
        return when (result) {
            is DomainSyncResult.Success -> DataSyncResult.Success(result.patientId, result.patientName)
            is DomainSyncResult.PendingApproval -> DataSyncResult.PendingApproval
            is DomainSyncResult.Error -> DataSyncResult.Error(result.message)
        }
    }
}

fun DataPairingRequest.toDomain(): DomainPairingRequest = PairingMapper.toDomain(this)
fun DomainPairingRequest.toData(): DataPairingRequest = PairingMapper.toData(this)
fun DataSyncResult.toDomain(): DomainSyncResult = PairingMapper.toDomain(this)
fun DomainSyncResult.toData(): DataSyncResult = PairingMapper.toData(this)

