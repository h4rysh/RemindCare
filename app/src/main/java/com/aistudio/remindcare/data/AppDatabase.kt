package com.aistudio.remindcare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Reminder::class, ReminderHistory::class, AppSettings::class, SyncOutboxEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun historyDao(): HistoryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Version 1→2: Schema version bump only.
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN connectedCaregiverName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN connectedCaregiverId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN connectedPatientName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN connectedPatientId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminder_history ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                
                val cursor = db.query("SELECT id FROM reminders")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    db.execSQL("UPDATE reminders SET uuid = '${java.util.UUID.randomUUID()}' WHERE id = $id")
                }
                cursor.close()
                
                val historyCursor = db.query("SELECT id FROM reminder_history")
                while (historyCursor.moveToNext()) {
                    val id = historyCursor.getLong(0)
                    db.execSQL("UPDATE reminder_history SET uuid = '${java.util.UUID.randomUUID()}' WHERE id = $id")
                }
                historyCursor.close()
                
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reminders_uuid ON reminders(uuid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reminder_history_uuid ON reminder_history(uuid)")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add soft-delete and timestamp tracking to reminders
                db.execSQL("ALTER TABLE reminders ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0")

                // Add soft-delete and timestamp tracking to reminder_history
                db.execSQL("ALTER TABLE reminder_history ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminder_history ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0")

                // Create sync outbox table for resilient offline mutation queue
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_outbox (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityUuid TEXT NOT NULL,
                        action TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remindcare_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
