package com.aistudio.remindcare.domain.repository

/**
 * Domain Gateway interface for saving and loading photo proofs.
 */
interface PhotoStorageGateway {
    suspend fun savePhotoProof(reminderId: Long, sourcePathOrUri: String): String?
    fun deletePhotoProof(photoPath: String)
}
