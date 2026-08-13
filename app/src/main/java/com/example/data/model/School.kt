package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class School(
    @PrimaryKey val schoolId: String,
    val sr: String = "",
    val district: String = "",
    val schoolName: String = "",
    val referenceCode: String = "", // Bracketed number parsed or explicit reference
    val type: String = "",
    val village: String = "",
    val principalName: String = "",
    val block: String = "",
    val mobile: String = "",
    val originalVisitDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
