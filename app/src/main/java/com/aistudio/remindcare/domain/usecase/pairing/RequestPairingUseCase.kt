package com.aistudio.remindcare.domain.usecase.pairing

import com.aistudio.remindcare.domain.model.SyncResult
import com.aistudio.remindcare.domain.repository.PairingRepository
import com.aistudio.remindcare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class RequestPairingUseCase(
    private val pairingRepository: PairingRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(code: String): Flow<SyncResult> {
        val settings = settingsRepository.getSettingsDirect()
        val caregiverName = if (settings.caregiverName.isNotBlank()) settings.caregiverName else "Caregiver"
        val caregiverId = pairingRepository.getDeviceId()
        return pairingRepository.requestPairing(code, caregiverId, caregiverName)
    }
}
