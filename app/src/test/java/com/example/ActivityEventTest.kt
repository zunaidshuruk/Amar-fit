package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.ActivityEvent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ActivityEventTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndQueryActivityEvents() = runBlocking {
        val now = System.currentTimeMillis()
        val dao = db.activityEventDao()

        val event1 = ActivityEvent(type = "water", description = "Logged 0.5L water", timestamp = now - 1000)
        val event2 = ActivityEvent(type = "sleep", description = "Logged 7.5h sleep", timestamp = now)

        dao.insert(event1)
        dao.insert(event2)

        val events = dao.getAllEvents().first()
        assertEquals(2, events.size)
        // Check order DESC by timestamp
        assertEquals("sleep", events[0].type)
        assertEquals("Logged 7.5h sleep", events[0].description)
        assertEquals("water", events[1].type)
        assertEquals("Logged 0.5L water", events[1].description)
    }

    @Test
    fun testGetTodayEventsFiltering() = runBlocking {
        val now = System.currentTimeMillis()
        val dao = db.activityEventDao()

        val startOfDay = now - 10000
        val endOfDay = now + 10000

        val insideEvent = ActivityEvent(type = "glucose", description = "Logged blood glucose: 5.6 mmol/L", timestamp = now)
        val outsideEvent = ActivityEvent(type = "weight", description = "Logged weight: 70kg", timestamp = now - 50000)

        dao.insert(insideEvent)
        dao.insert(outsideEvent)

        val todayEvents = dao.getTodayEvents(startOfDay, endOfDay).first()
        assertEquals(1, todayEvents.size)
        assertEquals("glucose", todayEvents[0].type)
    }

    @Test
    fun testMigration20To21Sql() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test-migration-20-21.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Simulating version 20 schema minimal table
                    db.execSQL("CREATE TABLE IF NOT EXISTS `daily_metrics` (`date` TEXT PRIMARY KEY NOT NULL, `caloriesConsumed` INTEGER NOT NULL)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqliteDb = helper.writableDatabase

        // Execute MIGRATION_20_21
        AppDatabase.MIGRATION_20_21.migrate(sqliteDb)

        // Insert into the newly created activity_events table
        sqliteDb.execSQL("INSERT INTO activity_events (type, description, timestamp) VALUES ('food', 'Logged Apple', 123456)")
        val cursor = sqliteDb.query("SELECT id, type, description, timestamp FROM activity_events WHERE type = 'food'")
        assertTrue(cursor.moveToFirst())
        assertEquals("food", cursor.getString(cursor.getColumnIndex("type")))
        assertEquals("Logged Apple", cursor.getString(cursor.getColumnIndex("description")))
        assertEquals(123456L, cursor.getLong(cursor.getColumnIndex("timestamp")))
        cursor.close()
        sqliteDb.close()
    }
}
