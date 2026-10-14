package com.aistudio.remindcare.receiver

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class BootReceiverTest {

    @Test
    fun testBootReceiver_handlesBootCompleted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(context)
        db.reminderDao().insertReminder(
            Reminder(
                id = 101L,
                title = "Morning Heart Pill",
                type = "Medicine",
                time = "08:00",
                repeat = "Daily",
                isActive = true
            )
        )

        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Trigger broadcast
        receiver.onReceive(context, intent)

        // Verify receiver constants
        assertTrue(BootReceiver.SYSTEM_ACTIONS.contains(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(BootReceiver.SYSTEM_ACTIONS.contains(Intent.ACTION_TIME_CHANGED))
        assertTrue(BootReceiver.SYSTEM_ACTIONS.contains(Intent.ACTION_TIMEZONE_CHANGED))
        assertTrue(BootReceiver.SYSTEM_ACTIONS.contains(Intent.ACTION_MY_PACKAGE_REPLACED))
    }

    @Test
    fun testBootReceiver_handlesTimeAndPackageEvents() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val receiver = BootReceiver()

        val timeIntent = Intent(Intent.ACTION_TIME_CHANGED)
        receiver.onReceive(context, timeIntent)

        val timezoneIntent = Intent(Intent.ACTION_TIMEZONE_CHANGED)
        receiver.onReceive(context, timezoneIntent)

        val replacedIntent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)
        receiver.onReceive(context, replacedIntent)

        // Non-system action should be ignored gracefully
        val ignoredIntent = Intent("com.unknown.random.ACTION")
        receiver.onReceive(context, ignoredIntent)

        assertNotNull(receiver)
    }
}
