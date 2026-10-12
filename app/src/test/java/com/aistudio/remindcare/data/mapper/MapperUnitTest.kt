package com.aistudio.remindcare.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import com.aistudio.remindcare.data.AppSettings as EntitySettings
import com.aistudio.remindcare.data.PairingRequest as EntityPairingRequest
import com.aistudio.remindcare.data.PairingStatus as EntityPairingStatus
import com.aistudio.remindcare.data.Reminder as EntityReminder
import com.aistudio.remindcare.data.ReminderHistory as EntityHistory
import com.aistudio.remindcare.data.SyncResult as EntitySyncResult
import com.aistudio.remindcare.domain.model.AppSettings as DomainSettings
import com.aistudio.remindcare.domain.model.PairingRequest as DomainPairingRequest
import com.aistudio.remindcare.domain.model.PairingStatus as DomainPairingStatus
import com.aistudio.remindcare.domain.model.Reminder as DomainReminder
import com.aistudio.remindcare.domain.model.ReminderHistory as DomainHistory
import com.aistudio.remindcare.domain.model.SyncResult as DomainSyncResult

class MapperUnitTest {

    @Test
    fun reminderEntityToDomain_preservesAllFields() {
        val entity = EntityReminder(
            id = 42L,
            uuid = "test-uuid-42",
            title = "Morning Blood Pressure",
            type = "Medicine",
            time = "08:30",
            repeat = "Daily",
            medicineName = "Amlodipine",
            medicineQuantity = "5mg (1 tablet)",
            medicineLocation = "Pill organizer, morning slot",
            photoRequired = true,
            additionalNotes = "Take with water",
            imageUri = "content://media/photos/1",
            isActive = true,
            soundUri = "content://media/audio/1",
            isDeleted = false,
            updatedAtMillis = 1700000000000L
        )

        val domain = entity.toDomain()

        assertThat(domain.id).isEqualTo(42L)
        assertThat(domain.uuid).isEqualTo("test-uuid-42")
        assertThat(domain.title).isEqualTo("Morning Blood Pressure")
        assertThat(domain.type).isEqualTo("Medicine")
        assertThat(domain.time).isEqualTo("08:30")
        assertThat(domain.repeat).isEqualTo("Daily")
        assertThat(domain.medicineName).isEqualTo("Amlodipine")
        assertThat(domain.medicineQuantity).isEqualTo("5mg (1 tablet)")
        assertThat(domain.medicineLocation).isEqualTo("Pill organizer, morning slot")
        assertThat(domain.photoRequired).isTrue()
        assertThat(domain.additionalNotes).isEqualTo("Take with water")
        assertThat(domain.imageUri).isEqualTo("content://media/photos/1")
        assertThat(domain.isActive).isTrue()
        assertThat(domain.soundUri).isEqualTo("content://media/audio/1")
        assertThat(domain.isDeleted).isFalse()
        assertThat(domain.updatedAtMillis).isEqualTo(1700000000000L)
    }

    @Test
    fun reminderDomainToEntity_roundtripIsIdentical() {
        val domain = DomainReminder(
            id = 99L,
            uuid = "uuid-99",
            title = "Evening Walk",
            type = "Exercise",
            time = "17:00",
            repeat = "Daily",
            medicineName = null,
            medicineQuantity = null,
            medicineLocation = null,
            photoRequired = false,
            additionalNotes = "Gentle 15m walk",
            imageUri = null,
            isActive = true,
            soundUri = null,
            isDeleted = false,
            updatedAtMillis = 1700000000500L
        )

        val entity = domain.toEntity()
        val roundtrip = entity.toDomain()

        assertThat(roundtrip).isEqualTo(domain)
    }

    @Test
    fun historyMapper_roundtripPreservesStatusAndTimestamps() {
        val domain = DomainHistory(
            id = 7L,
            reminderId = 42L,
            title = "Aspirin",
            type = "Medicine",
            scheduledTime = "2026-09-25 09:00",
            completedTime = "2026-09-25 09:05",
            status = "COMPLETED",
            photoPath = "/data/user/0/remindcare/photos/proof1.jpg",
            notes = "Taken after breakfast",
            isDeleted = false,
            updatedAtMillis = 1700000010000L
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()

        assertThat(restored.id).isEqualTo(7L)
        assertThat(restored.reminderId).isEqualTo(42L)
        assertThat(restored.title).isEqualTo("Aspirin")
        assertThat(restored.status).isEqualTo("COMPLETED")
        assertThat(restored.completedTime).isEqualTo("2026-09-25 09:05")
        assertThat(restored.photoPath).isEqualTo("/data/user/0/remindcare/photos/proof1.jpg")
        assertThat(restored.notes).isEqualTo("Taken after breakfast")
    }

    @Test
    fun settingsMapper_roundtripPreservesSeniorPreferences() {
        val domain = DomainSettings(
            id = 1,
            role = "Patient",
            patientName = "Eleanor Vance",
            caregiverName = "David Vance",
            isPaired = true,
            connectedCaregiverId = "caregiver-123",
            connectedCaregiverName = "David Vance",
            connectedPatientId = "patient-456",
            connectedPatientName = "Eleanor Vance",
            fontSize = "Very Large",
            themeMode = "Dark",
            reminderVolume = 0.95f,
            voiceInstructionsEnabled = true,
            caregiverPin = "pbkdf2\$abcdef123456"
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()

        assertThat(restored.fontSize).isEqualTo("Very Large")
        assertThat(restored.themeMode).isEqualTo("Dark")
        assertThat(restored.reminderVolume).isEqualTo(0.95f)
        assertThat(restored.voiceInstructionsEnabled).isTrue()
        assertThat(restored.caregiverPin).isEqualTo("pbkdf2\$abcdef123456")
        assertThat(restored.isPaired).isTrue()
    }

    @Test
    fun pairingMapper_statusTransformationsCorrect() {
        assertThat(PairingMapper.toDomain(EntityPairingStatus.PENDING)).isEqualTo(DomainPairingStatus.PENDING)
        assertThat(PairingMapper.toDomain(EntityPairingStatus.REQUESTED)).isEqualTo(DomainPairingStatus.REQUESTED)
        assertThat(PairingMapper.toDomain(EntityPairingStatus.APPROVED)).isEqualTo(DomainPairingStatus.APPROVED)
        assertThat(PairingMapper.toDomain(EntityPairingStatus.DECLINED)).isEqualTo(DomainPairingStatus.DECLINED)

        assertThat(PairingMapper.toData(DomainPairingStatus.PENDING)).isEqualTo(EntityPairingStatus.PENDING)
        assertThat(PairingMapper.toData(DomainPairingStatus.REQUESTED)).isEqualTo(EntityPairingStatus.REQUESTED)
        assertThat(PairingMapper.toData(DomainPairingStatus.APPROVED)).isEqualTo(EntityPairingStatus.APPROVED)
        assertThat(PairingMapper.toData(DomainPairingStatus.DECLINED)).isEqualTo(EntityPairingStatus.DECLINED)
    }

    @Test
    fun pairingMapper_requestAndResultRoundtrips() {
        val domainRequest = DomainPairingRequest(
            caregiverId = "cg-1",
            caregiverName = "Nurse Sarah",
            status = DomainPairingStatus.REQUESTED
        )
        val entityRequest = domainRequest.toData()
        val restoredRequest = entityRequest.toDomain()
        assertThat(restoredRequest.caregiverId).isEqualTo("cg-1")
        assertThat(restoredRequest.caregiverName).isEqualTo("Nurse Sarah")
        assertThat(restoredRequest.status).isEqualTo(DomainPairingStatus.REQUESTED)

        val successResult = DomainSyncResult.Success("pat-99", "Eleanor")
        val entitySuccess = successResult.toData()
        assertThat(entitySuccess).isInstanceOf(EntitySyncResult.Success::class.java)
        assertThat((entitySuccess as EntitySyncResult.Success).patientId).isEqualTo("pat-99")
        assertThat(entitySuccess.patientName).isEqualTo("Eleanor")
        assertThat(entitySuccess.toDomain()).isEqualTo(successResult)

        val pendingResult = DomainSyncResult.PendingApproval
        assertThat(pendingResult.toData().toDomain()).isEqualTo(DomainSyncResult.PendingApproval)

        val errorResult = DomainSyncResult.Error("Network Timeout")
        assertThat(errorResult.toData().toDomain()).isEqualTo(DomainSyncResult.Error("Network Timeout"))
    }
}
