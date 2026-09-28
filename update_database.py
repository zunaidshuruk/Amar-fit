import re

with open('app/src/main/java/com/example/data/local/AppDatabase.kt', 'r') as f:
    content = f.read()

# Update version to 12
content = re.sub(r"version\s*=\s*\d+", "version = 12", content)

# Add migration
migration_code = """
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserProfile::class, DailyMetric::class, FoodLog::class, SavedDietChart::class], version = 12, exportSchema = false)
"""
content = content.replace("@Database(entities = [UserProfile::class, DailyMetric::class, FoodLog::class, SavedDietChart::class], version = 12, exportSchema = false)", migration_code)

migration_obj = """
    companion object {
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN dateOfBirth TEXT NOT NULL DEFAULT ''")
            }
        }
"""
content = content.replace("    companion object {", migration_obj)

add_migration = """
                .addMigrations(MIGRATION_11_12)
                .build()
"""
content = content.replace("                .fallbackToDestructiveMigration()\n                .build()", add_migration)

with open('app/src/main/java/com/example/data/local/AppDatabase.kt', 'w') as f:
    f.write(content)
