package com.example.easydialer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Contact::class], version = 2)
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

        fun getDatabase(context: Context): ContactDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "contact_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
