package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TodayTileCustomizationTest {

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
    fun testDefaultTodayTileSlotsIsEmptyString() {
        val profile = UserProfile()
        assertEquals("", profile.todayTileSlots)
    }

    @Test
    fun testRoomPersistsCustomTodayTileSlots() = runBlocking {
        val userDao = db.userDao()
        val customProfile = UserProfile(
            id = 1,
            name = "Test User",
            todayTileSlots = "weight,sleep,blood_pressure"
        )

        userDao.insertProfile(customProfile)
        val loaded = userDao.getUserProfile().first()

        assertNotNull(loaded)
        assertEquals("weight,sleep,blood_pressure", loaded?.todayTileSlots)
    }

    @Test
    fun testMigration21To22AddsTodayTileSlotsColumn() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext<Context>())
            .name("migration-test-21-22.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(21) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `age` INTEGER NOT NULL, `onboardingCompleted` INTEGER NOT NULL, `dateOfBirth` TEXT NOT NULL, `gender` TEXT NOT NULL, `heightCm` REAL NOT NULL, `weightKg` REAL NOT NULL, `dietaryRestrictions` TEXT NOT NULL, `healthGoals` TEXT NOT NULL, `dailyCalorieLimit` INTEGER NOT NULL, `dailyWaterLimitLiters` REAL NOT NULL, `currentStreak` INTEGER NOT NULL, `points` INTEGER NOT NULL, `badges` TEXT NOT NULL, `lastActiveDate` TEXT NOT NULL, `profilePictureUri` TEXT, `isDarkMode` INTEGER NOT NULL, `notificationsEnabled` INTEGER NOT NULL, `remindersEnabled` INTEGER NOT NULL, `selectedLanguage` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("INSERT INTO `user_profile` (`id`, `name`, `age`, `onboardingCompleted`, `dateOfBirth`, `gender`, `heightCm`, `weightKg`, `dietaryRestrictions`, `healthGoals`, `dailyCalorieLimit`, `dailyWaterLimitLiters`, `currentStreak`, `points`, `badges`, `lastActiveDate`, `profilePictureUri`, `isDarkMode`, `notificationsEnabled`, `remindersEnabled`, `selectedLanguage`) VALUES (1, 'User 1', 30, 1, '1994-01-01', 'Female', 165.0, 60.0, 'None', 'Stay fit', 1800, 2.5, 5, 120, 'bronze', '2026-09-06', NULL, 0, 1, 1, 'English')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = helperFactory.create(config)
        val sqliteDb = openHelper.writableDatabase

        // Apply MIGRATION_21_22
        AppDatabase.MIGRATION_21_22.migrate(sqliteDb)

        val cursor = sqliteDb.query("SELECT todayTileSlots, name FROM user_profile WHERE id = 1")
        assertEquals(true, cursor.moveToFirst())
        val slotIndex = cursor.getColumnIndex("todayTileSlots")
        val nameIndex = cursor.getColumnIndex("name")
        assertEquals("", cursor.getString(slotIndex))
        assertEquals("User 1", cursor.getString(nameIndex))
        cursor.close()
        sqliteDb.close()
    }
}
