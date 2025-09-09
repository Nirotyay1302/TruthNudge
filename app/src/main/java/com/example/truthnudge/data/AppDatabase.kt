package com.example.truthnudge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Claim::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun claimDao(): ClaimDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Make sure table name matches your Claim entity
                database.execSQL(
                    "ALTER TABLE claims ADD COLUMN newColumn TEXT DEFAULT '' NOT NULL"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "truth_nudge_db"
                )
                    .addMigrations(MIGRATION_1_2) // optional: add more migrations for future versions
                    .fallbackToDestructiveMigration() // only if no migration path is provided
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
