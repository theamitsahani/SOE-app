package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class School(
    @PrimaryKey val schoolId: String,
    val stateName: String = "Rajasthan",
    val districtName: String = "",
    val schoolName: String = "",
    val schoolType: String = "",
    val villageName: String = "",
    val principalName: String = "",
    val blockName: String = "",
    val principalMobile: String = "",
    val visitDate: String = "",
    val sr: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Backward compatibility property getters
    val state: String get() = stateName
    val district: String get() = districtName
    val type: String get() = schoolType
    val village: String get() = villageName
    val block: String get() = blockName
    val mobile: String get() = principalMobile
    val originalVisitDate: String get() = visitDate
    val referenceCode: String get() = ""
}

