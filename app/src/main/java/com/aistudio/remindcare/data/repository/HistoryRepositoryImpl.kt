package com.aistudio.remindcare.data.repository

import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.SyncRepository
import com.aistudio.remindcare.data.mapper.toDomain
import com.aistudio.remindcare.data.mapper.toEntity
import com.aistudio.remindcare.domain.model.ReminderHistory
import com.aistudio.remindcare.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val database: AppDatabase,
    private val syncRepository: SyncRepository
) : HistoryRepository {

    private val historyDao = database.historyDao()

    override fun getAllHistory(): Flow<List<ReminderHistory>> {
        return historyDao.getAllHistory().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getHistoryDirect(): List<ReminderHistory> {
        return historyDao.getHistoryDirect().map { it.toDomain() }
    }

    override suspend fun insertHistory(history: ReminderHistory): Long {
        val entity = history.toEntity().copy(
            updatedAtMillis = System.currentTimeMillis(),
            isDeleted = false
        )
        historyDao.insertHistory(entity)
        return entity.id
    }

    override suspend fun updateHistory(history: ReminderHistory) {
        val entity = history.toEntity().copy(
            updatedAtMillis = System.currentTimeMillis()
        )
        historyDao.insertHistory(entity)
    }

    override suspend fun deleteHistory(history: ReminderHistory) {
        historyDao.softDeleteHistoryById(history.id, System.currentTimeMillis())
    }

    override suspend fun syncHistoryToCloud(patientId: String, history: List<ReminderHistory>) {
        syncRepository.syncHistoryToCloud(patientId, history.map { it.toEntity() })
    }

    override fun observeCloudHistory(patientId: String): Flow<List<ReminderHistory>> {
        return syncRepository.listenForCloudHistory(patientId).map { list -> list.map { it.toDomain() } }
    }
}
