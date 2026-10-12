package com.aistudio.remindcare.domain.usecase

import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.repository.AlarmSchedulerGateway
import com.aistudio.remindcare.domain.repository.ReminderRepository
import com.aistudio.remindcare.domain.usecase.reminder.*
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReminderUseCasesTest {

    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val alarmScheduler: AlarmSchedulerGateway = mockk(relaxed = true)

    private lateinit var getRemindersUseCase: GetRemindersUseCase
    private lateinit var getActiveRemindersUseCase: GetActiveRemindersUseCase
    private lateinit var getReminderByIdUseCase: GetReminderByIdUseCase
    private lateinit var saveReminderUseCase: SaveReminderUseCase
    private lateinit var deleteReminderUseCase: DeleteReminderUseCase
    private lateinit var prepopulateDefaultsUseCase: PrepopulateDefaultsUseCase

    @Before
    fun setup() {
        getRemindersUseCase = GetRemindersUseCase(reminderRepository)
        getActiveRemindersUseCase = GetActiveRemindersUseCase(reminderRepository)
        getReminderByIdUseCase = GetReminderByIdUseCase(reminderRepository)
        saveReminderUseCase = SaveReminderUseCase(reminderRepository, alarmScheduler)
        deleteReminderUseCase = DeleteReminderUseCase(reminderRepository, alarmScheduler)
        prepopulateDefaultsUseCase = PrepopulateDefaultsUseCase(reminderRepository)
    }

    @Test
    fun saveReminderUseCase_newReminder_insertsAndSchedulesAlarm() = runTest {
        val newReminder = Reminder(
            id = 0L,
            title = "Morning Heart Medicine",
            type = "Medicine",
            time = "08:00",
            repeat = "Daily",
            isActive = true
        )

        coEvery { reminderRepository.insertReminder(any()) } returns 101L

        val resultId = saveReminderUseCase(newReminder)

        assertThat(resultId).isEqualTo(101L)
        coVerify(exactly = 1) { reminderRepository.insertReminder(match { it.title == "Morning Heart Medicine" }) }
        coVerify(exactly = 0) { reminderRepository.updateReminder(any()) }
        verify(exactly = 1) { alarmScheduler.scheduleReminder(match { it.id == 101L }) }
        verify(exactly = 0) { alarmScheduler.cancelReminder(any()) }
    }

    @Test
    fun saveReminderUseCase_existingInactiveReminder_updatesAndCancelsAlarm() = runTest {
        val existingReminder = Reminder(
            id = 55L,
            title = "Old Reminder",
            type = "Water",
            time = "14:00",
            repeat = "Daily",
            isActive = false
        )

        val resultId = saveReminderUseCase(existingReminder)

        assertThat(resultId).isEqualTo(55L)
        coVerify(exactly = 1) { reminderRepository.updateReminder(match { it.id == 55L }) }
        coVerify(exactly = 0) { reminderRepository.insertReminder(any()) }
        verify(exactly = 1) { alarmScheduler.cancelReminder(55L) }
        verify(exactly = 0) { alarmScheduler.scheduleReminder(any()) }
    }

    @Test
    fun deleteReminderUseCase_cancelsAlarmAndSoftDeletes() = runTest {
        val reminder = Reminder(
            id = 88L,
            title = "Doctor Appointment",
            type = "Appointment",
            time = "11:00",
            repeat = "Once",
            isActive = true
        )

        deleteReminderUseCase(reminder)

        verify(exactly = 1) { alarmScheduler.cancelReminder(88L) }
        coVerify(exactly = 1) { reminderRepository.softDeleteReminder(88L) }
    }

    @Test
    fun getReminderByIdUseCase_returnsReminderFromRepository() = runTest {
        val reminder = Reminder(id = 12L, title = "Vitamin D", type = "Medicine", time = "10:00")
        coEvery { reminderRepository.getReminderById(12L) } returns reminder

        val result = getReminderByIdUseCase(12L)

        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo("Vitamin D")
        coVerify(exactly = 1) { reminderRepository.getReminderById(12L) }
    }

    @Test
    fun prepopulateDefaultsUseCase_delegatesToRepository() = runTest {
        prepopulateDefaultsUseCase()
        coVerify(exactly = 1) { reminderRepository.prepopulateDefaults() }
    }
}
