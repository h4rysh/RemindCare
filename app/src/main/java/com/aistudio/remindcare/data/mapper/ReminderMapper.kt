package com.aistudio.remindcare.data.mapper

import com.aistudio.remindcare.data.Reminder as ReminderEntity
import com.aistudio.remindcare.domain.model.Reminder as DomainReminder

object ReminderMapper {
    fun toDomain(entity: ReminderEntity): DomainReminder {
        return DomainReminder(
            id = entity.id,
            uuid = entity.uuid,
            title = entity.title,
            type = entity.type,
            time = entity.time,
            repeat = entity.repeat,
            medicineName = entity.medicineName,
            medicineQuantity = entity.medicineQuantity,
            medicineLocation = entity.medicineLocation,
            photoRequired = entity.photoRequired,
            additionalNotes = entity.additionalNotes,
            imageUri = entity.imageUri,
            isActive = entity.isActive,
            soundUri = entity.soundUri,
            isDeleted = entity.isDeleted,
            updatedAtMillis = entity.updatedAtMillis
        )
    }

    fun toEntity(domain: DomainReminder): ReminderEntity {
        return ReminderEntity(
            id = domain.id,
            uuid = domain.uuid,
            title = domain.title,
            type = domain.type,
            time = domain.time,
            repeat = domain.repeat,
            medicineName = domain.medicineName,
            medicineQuantity = domain.medicineQuantity,
            medicineLocation = domain.medicineLocation,
            photoRequired = domain.photoRequired,
            additionalNotes = domain.additionalNotes,
            imageUri = domain.imageUri,
            isActive = domain.isActive,
            soundUri = domain.soundUri,
            isDeleted = domain.isDeleted,
            updatedAtMillis = domain.updatedAtMillis
        )
    }
}

fun ReminderEntity.toDomain(): DomainReminder = ReminderMapper.toDomain(this)
fun DomainReminder.toEntity(): ReminderEntity = ReminderMapper.toEntity(this)
