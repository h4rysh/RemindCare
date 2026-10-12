package com.aistudio.remindcare.domain.usecase.pairing

import com.aistudio.remindcare.domain.model.PairingRequest
import com.aistudio.remindcare.domain.repository.PairingRepository
import kotlinx.coroutines.flow.Flow

class ListenPairingRequestUseCase(
    private val pairingRepository: PairingRepository
) {
    operator fun invoke(code: String): Flow<PairingRequest?> {
        return pairingRepository.listenForPairingRequest(code)
    }
}
