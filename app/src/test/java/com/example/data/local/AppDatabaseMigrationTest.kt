package com.example.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @Test
    fun migrate19To20() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration_test_19_20"
        context.deleteDatabase(dbName)

        // Create database at version 19
        val dbV19 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19
            )
            .build()
        dbV19.openHelper.writableDatabase.close()
        dbV19.close()

        // Migrate to version 20
        val dbV20 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20
            )
            .build()

        val db = dbV20.openHelper.writableDatabase
        AppDatabase.MIGRATION_19_20.migrate(db)

        // Verify table saved_chats exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='saved_chats'")
        assertTrue(cursor.moveToFirst())
        cursor.close()
        dbV20.close()
    }
}
