package com.aistudio.remindcare

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.remindcare.data.Reminder
import com.aistudio.remindcare.receiver.ReminderReceiver
import com.aistudio.remindcare.util.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class ReminderSchedulerTest {

    @Test
    fun testScheduleReminderDaily() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reminder = Reminder(id = 1, title = "Daily Meds", type = "Medicine", time = "08:00", repeat = "Daily", isActive = true)
        ReminderScheduler.scheduleReminder(context, reminder)

        val shadowAlarmManager = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isNotEmpty())
    }

    @Test
    fun testEarlyTriggerClampPreventsInfiniteLoop() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nowCal = Calendar.getInstance()
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = nowCal.get(Calendar.MINUTE)
        val timeStr = String.format("%02d:%02d", currentHour, currentMinute)

        // Schedule for the exact current minute (firing right now or within 2-min clamp window)
        val reminder = Reminder(id = 5, title = "Immediate Meds", type = "Medicine", time = timeStr, repeat = "Daily", isActive = true)
        ReminderScheduler.scheduleReminder(context, reminder)

        val shadowAlarmManager = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isNotEmpty())

        val scheduledTriggerTime = scheduledAlarms.first().triggerAtTime
        val now = System.currentTimeMillis()
        // Trigger time must be in the future (tomorrow, > 12 hours from now), NOT right now!
        assertTrue("Daily reminder within early trigger window must be scheduled for tomorrow", 
            scheduledTriggerTime > now + (12 * 60 * 60 * 1000L))
    }

    @Test
    fun testScheduleReminderWeeklyAdvancesSevenDays() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reminder = Reminder(id = 6, title = "Weekly Review", type = "Appointment", time = "00:01", repeat = "Weekly", isActive = true)
        ReminderScheduler.scheduleReminder(context, reminder)

        val shadowAlarmManager = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isNotEmpty())

        val triggerTime = scheduledAlarms.first().triggerAtTime
        val now = System.currentTimeMillis()
        assertTrue("Weekly reminder in past must advance forward", triggerTime > now)
    }

    @Test
    fun testScheduleReminderOncePastTimeNotScheduled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reminder = Reminder(id = 2, title = "Once Past", type = "Medicine", time = "00:00", repeat = "Once", isActive = true)
        ReminderScheduler.scheduleReminder(context, reminder)

        val shadowAlarmManager = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(0, scheduledAlarms.size)
    }

    @Test
    fun testCancelReminder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reminder = Reminder(id = 3, title = "Test Cancel", type = "General", time = "10:00", repeat = "Daily", isActive = true)
        ReminderScheduler.scheduleReminder(context, reminder)
        
        val shadowAlarmManager = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        
        ReminderScheduler.cancelReminder(context, 3L)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testDeterministicRequestCodesCollisionFree() {
        val code1Trigger = ReminderReceiver.getRequestCode(1L, ReminderReceiver.CODE_TRIGGER)
        val code1Complete = ReminderReceiver.getRequestCode(1L, ReminderReceiver.CODE_COMPLETE)
        val code1Snooze = ReminderReceiver.getRequestCode(1L, ReminderReceiver.CODE_SNOOZE)
        val code2Trigger = ReminderReceiver.getRequestCode(2L, ReminderReceiver.CODE_TRIGGER)

        // All codes must be positive 31-bit integers
        assertTrue(code1Trigger >= 0)
        assertTrue(code1Complete >= 0)
        assertTrue(code1Snooze >= 0)
        assertTrue(code2Trigger >= 0)

        // Codes for different actions on the same reminder must not collide
        assertNotEquals(code1Trigger, code1Complete)
        assertNotEquals(code1Trigger, code1Snooze)
        assertNotEquals(code1Complete, code1Snooze)

        // Codes for different reminders must not collide
        assertNotEquals(code1Trigger, code2Trigger)
    }

    @Test
    fun testScheduleReminderOneMinuteInFutureSchedulesToday() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val futureCal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 2)
        }
        val hour = futureCal.get(Calendar.HOUR_OF_DAY)
        val min = futureCal.get(Calendar.MINUTE)
        val timeStr = String.format("%02d:%02d", hour, min)

        val reminder = Reminder(id = 88, title = "Near Future Meds", type = "Medicine", time = timeStr, repeat = "Daily", isActive = true)
        ReminderScheduler.scheduleReminder(context, reminder)

        val shadowAlarmManager = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isNotEmpty())

        val scheduled = scheduledAlarms.first { shadowOf(it.operation).savedIntent.getLongExtra(ReminderReceiver.EXTRA_REMINDER_ID, -1L) == 88L }
        val now = System.currentTimeMillis()
        // Must be scheduled for today (within 3 minutes), NOT pushed to tomorrow (> 12 hours)
        assertTrue("Reminder 2 min in future must schedule today", scheduled.triggerAtTime <= now + (180 * 1000L))
    }
}
