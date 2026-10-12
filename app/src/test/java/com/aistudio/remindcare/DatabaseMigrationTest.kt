package com.aistudio.remindcare

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.Reminder
import com.aistudio.remindcare.data.SyncOutboxEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseMigrationTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSchemaVersion5IncludesOutboxAndSoftDelete() = runBlocking {
        // Test insert and fetch with version 5 schema fields
        val reminder = Reminder(
            title = "Blood Pressure Med",
            type = "Medicine",
            time = "09:00",
            repeat = "Daily",
            isDeleted = false,
            updatedAtMillis = 123456789L
        )
        val id = db.reminderDao().insertReminder(reminder)
        assertTrue(id > 0)

        val fetched = db.reminderDao().getReminderById(id)
        assertNotNull(fetched)
        assertEquals("Blood Pressure Med", fetched?.title)
        assertEquals(false, fetched?.isDeleted)
        assertEquals(123456789L, fetched?.updatedAtMillis)

        // Test outbox entity operations
        val outboxItem = SyncOutboxEntity(
            entityType = "REMINDER",
            entityUuid = fetched!!.uuid,
            action = "UPSERT",
            payloadJson = "{}"
        )
        val outboxId = db.outboxDao().insert(outboxItem)
        assertTrue(outboxId > 0)

        val pending = db.outboxDao().getAllPending()
        assertEquals(1, pending.size)
        assertEquals(fetched.uuid, pending[0].entityUuid)

        db.outboxDao().deleteById(outboxId)
        val remaining = db.outboxDao().getAllPending()
        assertEquals(0, remaining.size)
    }

    @Test
    fun testSoftDeleteHidesFromActiveQuery() = runBlocking {
        val reminder = Reminder(
            title = "Physical Therapy",
            type = "Exercise",
            time = "14:00",
            repeat = "Daily",
            isDeleted = false
        )
        val id = db.reminderDao().insertReminder(reminder)

        val activeList = db.reminderDao().getActiveRemindersDirect()
        assertTrue(activeList.any { it.id == id })

        // Perform soft delete
        db.reminderDao().softDeleteReminderById(id)

        val activeAfter = db.reminderDao().getActiveRemindersDirect()
        assertTrue(activeAfter.none { it.id == id })
    }
}
