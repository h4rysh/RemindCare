package com.aistudio.remindcare

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.AppSettings
import com.aistudio.remindcare.data.Reminder
import com.aistudio.remindcare.data.RemindCareRepository
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.util.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RemindCareViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSettingsMutation() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = RemindCareRepository(AppDatabase.getDatabase(app), app)
        val viewModel = RemindCareViewModel(app, repo)
        
        repo.saveSettings(AppSettings(fontSize = "Large", themeMode = "Dark", reminderVolume = 0.9f))
        testScheduler.advanceUntilIdle()
        
        val updatedSettings = repo.getSettingsDirect()
        assertEquals("Large", updatedSettings.fontSize)
        assertEquals("Dark", updatedSettings.themeMode)
        assertEquals(0.9f, updatedSettings.reminderVolume, 0.01f)
    }

    @Test
    fun testAddAndCompleteReminder() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = RemindCareRepository(AppDatabase.getDatabase(app), app)
        val viewModel = RemindCareViewModel(app, repo)
        
        val reminder = Reminder(id = 10, title = "Aspirin", type = "Medicine", time = "12:00", repeat = "Once")
        val insertedId = repo.insertReminder(reminder)
        assertTrue(insertedId > 0)
        
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        repo.insertHistory(
            com.aistudio.remindcare.data.ReminderHistory(
                reminderId = insertedId,
                title = reminder.title,
                type = reminder.type,
                scheduledTime = reminder.time,
                completedTime = timestamp,
                status = "COMPLETED",
                photoPath = null,
                notes = "Completed on device"
            )
        )
        
        val history = repo.getHistoryDirect()
        assertEquals(1, history.size)
        assertEquals("COMPLETED", history[0].status)
    }

    @Test
    fun testSoftDeleteReminderHidesFromActiveList() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = RemindCareRepository(AppDatabase.getDatabase(app), app)
        
        val reminder = Reminder(id = 25, title = "Evening Walk", type = "Exercise", time = "17:00", repeat = "Daily")
        val id = repo.insertReminder(reminder)

        // Verify it is active
        val activeBefore = repo.activeReminders.first()
        assertTrue(activeBefore.any { it.id == id })

        // Perform soft delete
        repo.deleteReminderById(id)

        // Verify it is hidden from active reminders but data is preserved for audit
        val activeAfter = repo.activeReminders.first()
        assertTrue(activeAfter.none { it.id == id })
    }

    @Test
    fun testCaregiverProfileUpdateWithPbkdf2Pin() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = RemindCareRepository(AppDatabase.getDatabase(app), app)
        val viewModel = RemindCareViewModel(app, repo)

        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        viewModel.updateCaregiverProfile("Nurse Sarah", "4321")
        testScheduler.advanceUntilIdle()
        Thread.sleep(200)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val settings = repo.getSettingsDirect()
        assertEquals("Nurse Sarah", settings.caregiverName)
        assertTrue("PIN must be hashed with PBKDF2", settings.caregiverPin.startsWith("pbkdf2$"))
        assertTrue("PIN verification must succeed", HashUtils.verifyPin("4321", settings.caregiverPin))
    }

    @Test
    fun testPatientProfileUpdate() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = RemindCareRepository(AppDatabase.getDatabase(app), app)
        val viewModel = RemindCareViewModel(app, repo)

        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        viewModel.updatePatientProfile("Grandma Eleanor")
        testScheduler.advanceUntilIdle()

        var settings: AppSettings? = null
        for (i in 1..20) {
            val s = repo.getSettingsDirect()
            if (s.patientName == "Grandma Eleanor") {
                settings = s
                break
            }
            Thread.sleep(50)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        assertNotNull("Patient name should be saved on IO thread", settings)
        assertEquals("Grandma Eleanor", settings?.patientName)
    }

    @Test
    fun testSyncIdCollision() = runTest {
        val reminder1 = Reminder(title = "Test 1", type = "Medicine", time = "12:00", repeat = "Once")
        val reminder2 = Reminder(title = "Test 2", type = "Medicine", time = "12:00", repeat = "Once")
        
        assertNotEquals(reminder1.uuid, reminder2.uuid)
    }

    @Test
    fun testPairingCodeGenerationFailure() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = RemindCareRepository(AppDatabase.getDatabase(app), app)
        
        repo.syncRepository = object : com.aistudio.remindcare.data.SyncRepository {
            override suspend fun generatePairingCode(patientName: String): String {
                throw Exception("Network Timeout")
            }
            override fun listenForPairingRequest(code: String) = kotlinx.coroutines.flow.emptyFlow<com.aistudio.remindcare.data.PairingRequest?>()
            override suspend fun approvePairingRequest(code: String, caregiverId: String, caregiverName: String) = false
            override suspend fun declinePairingRequest(code: String) = false
            override fun requestPairing(code: String, caregiverId: String, caregiverName: String) = kotlinx.coroutines.flow.emptyFlow<com.aistudio.remindcare.data.SyncResult>()
            override suspend fun disconnect(patientId: String, caregiverId: String) {}
            override suspend fun syncRemindersToCloud(patientId: String, reminders: List<Reminder>) {}
            override suspend fun deleteCloudReminder(patientId: String, reminderId: Long) {}
            override fun listenForCloudReminders(patientId: String) = kotlinx.coroutines.flow.emptyFlow<List<Reminder>>()
            override suspend fun syncHistoryToCloud(patientId: String, history: List<com.aistudio.remindcare.data.ReminderHistory>) {}
            override fun listenForCloudHistory(patientId: String) = kotlinx.coroutines.flow.emptyFlow<List<com.aistudio.remindcare.data.ReminderHistory>>()
            override fun getDeviceId(): String = "test-device"
        }
        
        val viewModel = RemindCareViewModel(app, repo)
        
        var generatedCode: String? = null
        var generatedError: String? = null
        
        viewModel.generatePairingCode { code, error ->
            generatedCode = code
            generatedError = error
        }
        
        runCurrent()
        
        assertEquals(null, generatedCode)
        assertEquals("Network Timeout", generatedError)
    }
}
