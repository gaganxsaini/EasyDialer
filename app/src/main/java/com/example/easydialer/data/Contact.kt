package com.example.easydialer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_table")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "imagePath") val imagePath: String?
)