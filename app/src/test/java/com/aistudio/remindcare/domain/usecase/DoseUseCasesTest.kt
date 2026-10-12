package com.aistudio.remindcare.domain.usecase

import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.model.ReminderHistory
import com.aistudio.remindcare.domain.repository.AlarmSchedulerGateway
import com.aistudio.remindcare.domain.repository.HistoryRepository
import com.aistudio.remindcare.domain.repository.PhotoStorageGateway
import com.aistudio.remindcare.domain.repository.ReminderRepository
import com.aistudio.remindcare.domain.usecase.dose.CheckMissedRemindersUseCase
import com.aistudio.remindcare.domain.usecase.dose.CompleteReminderUseCase
import com.aistudio.remindcare.domain.usecase.dose.SnoozeReminderUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DoseUseCasesTest {

    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val historyRepository: HistoryRepository = mockk(relaxed = true)
    private val alarmScheduler: AlarmSchedulerGateway = mockk(relaxed = true)
    private val photoStorage: PhotoStorageGateway = mockk(relaxed = true)

    private lateinit var completeReminderUseCase: CompleteReminderUseCase
    private lateinit var snoozeReminderUseCase: SnoozeReminderUseCase
    private lateinit var checkMissedRemindersUseCase: CheckMissedRemindersUseCase

    @Before
    fun setup() {
        completeReminderUseCase = CompleteReminderUseCase(
            reminderRepository = reminderRepository,
            historyRepository = historyRepository,
            alarmScheduler = alarmScheduler,
            photoStorage = photoStorage
        )
        snoozeReminderUseCase = SnoozeReminderUseCase(
            reminderRepository = reminderRepository,
            historyRepository = historyRepository,
            alarmScheduler = alarmScheduler
        )
        checkMissedRemindersUseCase = CheckMissedRemindersUseCase(
            reminderRepository = reminderRepository,
            historyRepository = historyRepository
        )
    }

    @Test
    fun completeReminderUseCase_recurringReminder_logsHistoryAndReschedules() = runTest {
        val reminder = Reminder(
            id = 10L,
            title = "Blood Thinner",
            type = "Medicine",
            time = "09:00",
            repeat = "Daily",
            isActive = true
        )
        coEvery { reminderRepository.getReminderById(10L) } returns reminder

        val history = completeReminderUseCase(10L, photoPathOrUri = null, notes = "Taken with juice")

        assertThat(history).isNotNull()
        assertThat(history?.status).isEqualTo("COMPLETED")
        assertThat(history?.reminderId).isEqualTo(10L)
        assertThat(history?.notes).isEqualTo("Taken with juice")

        coVerify(exactly = 1) { historyRepository.insertHistory(match { it.status == "COMPLETED" }) }
        verify(exactly = 1) { alarmScheduler.scheduleReminder(match { it.id == 10L }) }
        verify(exactly = 0) { alarmScheduler.cancelReminder(any()) }
    }

    @Test
    fun completeReminderUseCase_onceReminder_deactivatesAndCancelsAlarm() = runTest {
        val reminder = Reminder(
            id = 20L,
            title = "Covid Vaccine",
            type = "Appointment",
            time = "15:00",
            repeat = "Once",
            isActive = true
        )
        coEvery { reminderRepository.getReminderById(20L) } returns reminder

        val history = completeReminderUseCase(20L, photoPathOrUri = null)

        assertThat(history?.status).isEqualTo("COMPLETED")
        coVerify(exactly = 1) { reminderRepository.updateReminder(match { it.id == 20L && !it.isActive }) }
        verify(exactly = 1) { alarmScheduler.cancelReminder(20L) }
    }

    @Test
    fun completeReminderUseCase_withPhotoProof_persistsPhotoPath() = runTest {
        val reminder = Reminder(id = 30L, title = "Insulin", type = "Medicine", time = "07:30")
        coEvery { reminderRepository.getReminderById(30L) } returns reminder
        coEvery { photoStorage.savePhotoProof(30L, "temp_proof.jpg") } returns "/photos/permanent_30.jpg"

        val history = completeReminderUseCase(30L, photoPathOrUri = "temp_proof.jpg")

        assertThat(history?.photoPath).isEqualTo("/photos/permanent_30.jpg")
        coVerify(exactly = 1) { photoStorage.savePhotoProof(30L, "temp_proof.jpg") }
    }

    @Test
    fun snoozeReminderUseCase_schedules5MinSnoozeAndLogsHistory() = runTest {
        val reminder = Reminder(id = 40L, title = "Lunch Pills", type = "Medicine", time = "12:00")
        coEvery { reminderRepository.getReminderById(40L) } returns reminder

        val history = snoozeReminderUseCase(40L, snoozeMinutes = 5)

        assertThat(history).isNotNull()
        assertThat(history?.status).isEqualTo("SNOOZED")
        assertThat(history?.reminderId).isEqualTo(40L)
        coVerify(exactly = 1) { historyRepository.insertHistory(match { it.status == "SNOOZED" }) }
        verify(exactly = 1) { alarmScheduler.scheduleReminder(match { it.id == 40L }) }
    }
}
