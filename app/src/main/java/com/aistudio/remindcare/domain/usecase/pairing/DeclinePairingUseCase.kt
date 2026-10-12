package com.aistudio.remindcare.domain.usecase.pairing

import com.aistudio.remindcare.domain.repository.PairingRepository

class DeclinePairingUseCase(
    private val pairingRepository: PairingRepository
) {
    suspend operator fun invoke(code: String): Boolean {
        return pairingRepository.declinePairingRequest(code)
    }
}
