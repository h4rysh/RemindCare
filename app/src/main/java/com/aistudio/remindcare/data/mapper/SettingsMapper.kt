package com.aistudio.remindcare.data.mapper

import com.aistudio.remindcare.data.AppSettings as SettingsEntity
import com.aistudio.remindcare.domain.model.AppSettings as DomainSettings

object SettingsMapper {
    fun toDomain(entity: SettingsEntity): DomainSettings {
        return DomainSettings(
            id = entity.id,
            role = entity.role,
            patientName = entity.patientName,
            caregiverName = entity.caregiverName,
            caregiverPin = entity.caregiverPin,
            fontSize = entity.fontSize,
            themeMode = entity.themeMode,
            reminderVolume = entity.reminderVolume,
            ringtone = entity.ringtone,
            voiceInstructionsEnabled = entity.voiceInstructionsEnabled,
            isPaired = entity.isPaired,
            connectedCaregiverName = entity.connectedCaregiverName,
            connectedCaregiverId = entity.connectedCaregiverId,
            connectedPatientName = entity.connectedPatientName,
            connectedPatientId = entity.connectedPatientId
        )
    }

    fun toEntity(domain: DomainSettings): SettingsEntity {
        return SettingsEntity(
            id = domain.id,
            role = domain.role,
            patientName = domain.patientName,
            caregiverName = domain.caregiverName,
            caregiverPin = domain.caregiverPin,
            fontSize = domain.fontSize,
            themeMode = domain.themeMode,
            reminderVolume = domain.reminderVolume,
            ringtone = domain.ringtone,
            voiceInstructionsEnabled = domain.voiceInstructionsEnabled,
            isPaired = domain.isPaired,
            connectedCaregiverName = domain.connectedCaregiverName,
            connectedCaregiverId = domain.connectedCaregiverId,
            connectedPatientName = domain.connectedPatientName,
            connectedPatientId = domain.connectedPatientId
        )
    }
}

fun SettingsEntity.toDomain(): DomainSettings = SettingsMapper.toDomain(this)
fun DomainSettings.toEntity(): SettingsEntity = SettingsMapper.toEntity(this)
