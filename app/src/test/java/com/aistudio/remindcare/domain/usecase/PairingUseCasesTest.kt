package com.aistudio.remindcare.domain.usecase

import com.aistudio.remindcare.domain.model.AppSettings
import com.aistudio.remindcare.domain.model.PairingRequest
import com.aistudio.remindcare.domain.model.PairingStatus
import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.model.ReminderHistory
import com.aistudio.remindcare.domain.model.SyncResult
import com.aistudio.remindcare.domain.repository.HistoryRepository
import com.aistudio.remindcare.domain.repository.PairingRepository
import com.aistudio.remindcare.domain.repository.ReminderRepository
import com.aistudio.remindcare.domain.repository.SettingsRepository
import com.aistudio.remindcare.domain.usecase.pairing.*
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class PairingUseCasesTest {

    private val pairingRepository: PairingRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val historyRepository: HistoryRepository = mockk(relaxed = true)

    private lateinit var generatePairingCodeUseCase: GeneratePairingCodeUseCase
    private lateinit var requestPairingUseCase: RequestPairingUseCase
    private lateinit var listenPairingRequestUseCase: ListenPairingRequestUseCase
    private lateinit var approvePairingUseCase: ApprovePairingUseCase
    private lateinit var declinePairingUseCase: DeclinePairingUseCase
    private lateinit var disconnectPairingUseCase: DisconnectPairingUseCase

    @Before
    fun setup() {
        generatePairingCodeUseCase = GeneratePairingCodeUseCase(pairingRepository, settingsRepository)
        requestPairingUseCase = RequestPairingUseCase(pairingRepository, settingsRepository)
        listenPairingRequestUseCase = ListenPairingRequestUseCase(pairingRepository)
        approvePairingUseCase = ApprovePairingUseCase(
            pairingRepository = pairingRepository,
            settingsRepository = settingsRepository,
            reminderRepository = reminderRepository,
            historyRepository = historyRepository
        )
        declinePairingUseCase = DeclinePairingUseCase(pairingRepository)
        disconnectPairingUseCase = DisconnectPairingUseCase(pairingRepository, settingsRepository)
    }

    @Test
    fun generatePairingCodeUseCase_returnsCodeFromRepository() = runTest {
        coEvery { pairingRepository.generatePairingCode("Eleanor") } returns "849201"

        val code = generatePairingCodeUseCase("Eleanor")

        assertThat(code).isEqualTo("849201")
        coVerify(exactly = 1) { pairingRepository.generatePairingCode("Eleanor") }
    }

    @Test
    fun approvePairingUseCase_success_updatesSettingsAndPushesData() = runTest {
        coEvery { pairingRepository.approvePairingRequest("123456", "cg-42", "Nurse Sarah") } returns true
        coEvery { pairingRepository.getDeviceId() } returns "device-patient-1"
        coEvery { settingsRepository.getSettingsDirect() } returns AppSettings(role = "Patient")
        coEvery { reminderRepository.getRemindersDirect() } returns listOf(Reminder(id = 1L, title = "Meds", time = "08:00"))
        coEvery { historyRepository.getHistoryDirect() } returns listOf(ReminderHistory(id = 1L, title = "Meds", scheduledTime = "2026-09-25 08:00", status = "COMPLETED"))

        val success = approvePairingUseCase("123456", "cg-42", "Nurse Sarah")

        assertThat(success).isTrue()
        coVerify(exactly = 1) {
            settingsRepository.updateSettings(match {
                it.isPaired &&
                it.connectedCaregiverId == "cg-42" &&
                it.connectedCaregiverName == "Nurse Sarah"
            })
        }
        coVerify(exactly = 1) { reminderRepository.syncRemindersToCloud("device-patient-1", any()) }
        coVerify(exactly = 1) { historyRepository.syncHistoryToCloud("device-patient-1", any()) }
    }

    @Test
    fun declinePairingUseCase_delegatesToRepository() = runTest {
        coEvery { pairingRepository.declinePairingRequest("654321") } returns true

        val declined = declinePairingUseCase("654321")

        assertThat(declined).isTrue()
        coVerify(exactly = 1) { pairingRepository.declinePairingRequest("654321") }
    }

    @Test
    fun disconnectPairingUseCase_clearsSettingsAndCallsDisconnect() = runTest {
        coEvery { settingsRepository.getSettingsDirect() } returns AppSettings(
            isPaired = true,
            connectedPatientId = "pat-1",
            connectedCaregiverId = "cg-1"
        )

        disconnectPairingUseCase()

        coVerify(exactly = 1) { pairingRepository.disconnect("pat-1", "cg-1") }
        coVerify(exactly = 1) {
            settingsRepository.updateSettings(match {
                !it.isPaired &&
                it.connectedCaregiverId.isEmpty() &&
                it.connectedPatientId.isEmpty()
            })
        }
    }
}
