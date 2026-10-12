package com.aistudio.remindcare.domain.usecase

import com.aistudio.remindcare.domain.model.AppSettings
import com.aistudio.remindcare.domain.repository.SettingsRepository
import com.aistudio.remindcare.domain.usecase.auth.SetCaregiverPinUseCase
import com.aistudio.remindcare.domain.usecase.auth.VerifyCaregiverPinUseCase
import com.aistudio.remindcare.util.HashUtils
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AuthUseCasesTest {

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private lateinit var verifyCaregiverPinUseCase: VerifyCaregiverPinUseCase
    private lateinit var setCaregiverPinUseCase: SetCaregiverPinUseCase

    @Before
    fun setup() {
        verifyCaregiverPinUseCase = VerifyCaregiverPinUseCase(settingsRepository)
        setCaregiverPinUseCase = SetCaregiverPinUseCase(settingsRepository)
    }

    @Test
    fun verifyCaregiverPinUseCase_correctPin_returnsTrue() = runTest {
        val hashedPin = HashUtils.hashPin("1234")
        coEvery { settingsRepository.getSettingsDirect() } returns AppSettings(caregiverPin = hashedPin)

        val result = verifyCaregiverPinUseCase("1234")

        assertThat(result).isTrue()
    }

    @Test
    fun verifyCaregiverPinUseCase_wrongPin_returnsFalse() = runTest {
        val hashedPin = HashUtils.hashPin("1234")
        coEvery { settingsRepository.getSettingsDirect() } returns AppSettings(caregiverPin = hashedPin)

        val result = verifyCaregiverPinUseCase("9999")

        assertThat(result).isFalse()
    }

    @Test
    fun verifyCaregiverPinUseCase_emptyStoredPin_allowsAccess() = runTest {
        coEvery { settingsRepository.getSettingsDirect() } returns AppSettings(caregiverPin = "")

        val result = verifyCaregiverPinUseCase("1234")

        assertThat(result).isTrue()
    }

    @Test
    fun verifyCaregiverPinUseCase_blankInputPin_returnsFalse() = runTest {
        val hashedPin = HashUtils.hashPin("1234")
        coEvery { settingsRepository.getSettingsDirect() } returns AppSettings(caregiverPin = hashedPin)

        assertThat(verifyCaregiverPinUseCase("")).isFalse()
        assertThat(verifyCaregiverPinUseCase("   ")).isFalse()
    }

    @Test
    fun setCaregiverPinUseCase_valid4DigitPin_hashesAndSaves() = runTest {
        coEvery { settingsRepository.getSettingsDirect() } returns AppSettings(caregiverPin = "")

        val success = setCaregiverPinUseCase("4321")

        assertThat(success).isTrue()
        coVerify(exactly = 1) {
            settingsRepository.updateSettings(match {
                it.caregiverPin.startsWith("pbkdf2\$") &&
                HashUtils.verifyPin("4321", it.caregiverPin)
            })
        }
    }

    @Test
    fun setCaregiverPinUseCase_invalidLength_returnsFalse() = runTest {
        val tooShort = setCaregiverPinUseCase("12")
        val tooLong = setCaregiverPinUseCase("12345")

        assertThat(tooShort).isFalse()
        assertThat(tooLong).isFalse()
        coVerify(exactly = 0) { settingsRepository.updateSettings(any()) }
    }
}
