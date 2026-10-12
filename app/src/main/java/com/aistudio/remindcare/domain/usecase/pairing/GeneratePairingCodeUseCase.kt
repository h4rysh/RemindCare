package com.aistudio.remindcare.domain.usecase.pairing

import com.aistudio.remindcare.domain.repository.PairingRepository
import com.aistudio.remindcare.domain.repository.SettingsRepository

class GeneratePairingCodeUseCase(
    private val pairingRepository: PairingRepository,
    private val settingsRepository: SettingsRepository? = null
) {
    suspend operator fun invoke(patientName: String = "Patient"): String {
        val name = patientName.ifBlank { "Patient" }
        return pairingRepository.generatePairingCode(name)
    }
}
