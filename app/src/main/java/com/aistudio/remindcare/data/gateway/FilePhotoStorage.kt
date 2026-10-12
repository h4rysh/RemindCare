package com.aistudio.remindcare.data.gateway

import android.content.Context
import android.net.Uri
import com.aistudio.remindcare.domain.repository.PhotoStorageGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FilePhotoStorage(private val context: Context) : PhotoStorageGateway {

    override suspend fun savePhotoProof(reminderId: Long, sourcePathOrUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val photosDir = File(context.filesDir, "medication_proofs")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }

            val targetFile = File(photosDir, "proof_${reminderId}_${UUID.randomUUID()}.jpg")

            if (sourcePathOrUri.startsWith("content://") || sourcePathOrUri.startsWith("file://")) {
                val uri = Uri.parse(sourcePathOrUri)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                val sourceFile = File(sourcePathOrUri)
                if (sourceFile.exists()) {
                    sourceFile.copyTo(targetFile, overwrite = true)
                } else {
                    return@withContext sourcePathOrUri
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("FilePhotoStorage", "Failed to save photo proof", e)
            sourcePathOrUri
        }
    }

    override fun deletePhotoProof(photoPath: String) {
        try {
            val file = File(photoPath)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("FilePhotoStorage", "Failed to delete photo proof", e)
        }
    }
}
