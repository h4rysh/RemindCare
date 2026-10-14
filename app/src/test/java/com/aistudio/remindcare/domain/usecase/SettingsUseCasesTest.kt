package com.aistudio.remindcare.domain.usecase

import com.aistudio.remindcare.domain.model.AppSettings
import com.aistudio.remindcare.domain.repository.SettingsRepository
import com.aistudio.remindcare.domain.usecase.settings.GetSettingsUseCase
import com.aistudio.remindcare.domain.usecase.settings.UpdateCaregiverProfileUseCase
import com.aistudio.remindcare.domain.usecase.settings.UpdatePatientProfileUseCase
import com.aistudio.remindcare.domain.usecase.settings.UpdateSettingsUseCase
import com.aistudio.remindcare.util.HashUtils
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SettingsUseCasesTest {

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateSettingsUseCase: UpdateSettingsUseCase
    private lateinit var updatePatientProfileUseCase: UpdatePatientProfileUseCase
    private lateinit var updateCaregiverProfileUseCase: UpdateCaregiverProfileUseCase

    @Before
    fun setup() {
        getSettingsUseCase = GetSettingsUseCase(settingsRepository)
        updateSettingsUseCase = UpdateSettingsUseCase(settingsRepository)
        updatePatientProfileUseCase = UpdatePatientProfileUseCase(settingsRepository)
        updateCaregiverProfileUseCase = UpdateCaregiverProfileUseCase(settingsRepository)
    }

    @Test
    fun getSettingsUseCase_returnsFlowAndDirect() = runTest {
        val settings = AppSettings(fontSize = "Large", themeMode = "Dark")
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.getSettingsDirect() } returns settings

        val flowResult = getSettingsUseCase().first()
        val directResult = getSettingsUseCase.getDirect()

        assertThat(flowResult.fontSize).isEqualTo("Large")
        assertThat(directResult.themeMode).isEqualTo("Dark")
    }

    @Test
    fun updatePatientProfileUseCase_updatesPatientNameAndPersistsRole() = runTest {
        val existing = AppSettings(patientName = "Old Name", fontSize = "Large")
        coEvery { settingsRepository.getSettingsDirect() } returns existing

        updatePatientProfileUseCase("Eleanor Vance")

        coVerify(exactly = 1) {
            settingsRepository.updateSettings(match {
                it.patientName == "Eleanor Vance" && it.fontSize == "Large" && it.role == "Patient"
            })
        }
    }

    @Test
    fun updateCaregiverProfileUseCase_hashesPinAndPersistsRole() = runTest {
        val existing = AppSettings(caregiverName = "Old Name")
        coEvery { settingsRepository.getSettingsDirect() } returns existing

        updateCaregiverProfileUseCase("David Vance", "5678")

        coVerify(exactly = 1) {
            settingsRepository.updateSettings(match {
                it.caregiverName == "David Vance" &&
                it.caregiverPin.startsWith("pbkdf2\$") &&
                HashUtils.verifyPin("5678", it.caregiverPin) &&
                it.role == "Caregiver"
            })
        }
    }
}
