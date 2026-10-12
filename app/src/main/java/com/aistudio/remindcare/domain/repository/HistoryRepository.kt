package com.aistudio.remindcare.domain.repository

import com.aistudio.remindcare.domain.model.ReminderHistory
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for Medication and Reminder History logs.
 * Pure Kotlin contract.
 */
interface HistoryRepository {
    fun getAllHistory(): Flow<List<ReminderHistory>>
    suspend fun getHistoryDirect(): List<ReminderHistory>
    suspend fun insertHistory(history: ReminderHistory): Long
    suspend fun updateHistory(history: ReminderHistory)
    suspend fun deleteHistory(history: ReminderHistory)
    suspend fun syncHistoryToCloud(patientId: String, history: List<ReminderHistory>)
    fun observeCloudHistory(patientId: String): Flow<List<ReminderHistory>>
}
