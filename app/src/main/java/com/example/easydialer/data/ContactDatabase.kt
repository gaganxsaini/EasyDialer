package com.example.easydialer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Contact::class], version = 4)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: ContactDatabase? = null

        /** Adds the columns that keep a contact working after it is edited. Existing rows are kept. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contact_table ADD COLUMN lookup_key TEXT")
                db.execSQL("ALTER TABLE contact_table ADD COLUMN number TEXT")
                db.execSQL("ALTER TABLE contact_table ADD COLUMN name TEXT")
            }
        }

        /** Adds the custom photo column. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contact_table ADD COLUMN custom_photo TEXT")
            }
        }

        /** Adds the order column; existing contacts keep the order they were added in. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contact_table ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE contact_table SET position = id")
            }
        }

        fun getDatabase(context: Context): ContactDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "contact_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
