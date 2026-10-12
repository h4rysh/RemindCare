package com.aistudio.remindcare.data.mapper

import com.aistudio.remindcare.data.ReminderHistory as HistoryEntity
import com.aistudio.remindcare.domain.model.ReminderHistory as DomainHistory

object HistoryMapper {
    fun toDomain(entity: HistoryEntity): DomainHistory {
        return DomainHistory(
            id = entity.id,
            uuid = entity.uuid,
            reminderId = entity.reminderId,
            title = entity.title,
            type = entity.type,
            scheduledTime = entity.scheduledTime,
            completedTime = entity.completedTime,
            status = entity.status,
            photoPath = entity.photoPath,
            notes = entity.notes,
            isDeleted = entity.isDeleted,
            updatedAtMillis = entity.updatedAtMillis
        )
    }

    fun toEntity(domain: DomainHistory): HistoryEntity {
        return HistoryEntity(
            id = domain.id,
            uuid = domain.uuid,
            reminderId = domain.reminderId,
            title = domain.title,
            type = domain.type,
            scheduledTime = domain.scheduledTime,
            completedTime = domain.completedTime,
            status = domain.status,
            photoPath = domain.photoPath,
            notes = domain.notes,
            isDeleted = domain.isDeleted,
            updatedAtMillis = domain.updatedAtMillis
        )
    }
}

fun HistoryEntity.toDomain(): DomainHistory = HistoryMapper.toDomain(this)
fun DomainHistory.toEntity(): HistoryEntity = HistoryMapper.toEntity(this)
