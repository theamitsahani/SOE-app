package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VisitStatus {
    ASSIGNED,
    STARTED,
    IN_PROGRESS,
    SUBMITTED,
    REVIEWED
}

enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED
}

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey val visitId: String,
    val schoolId: String,
    val employeeId: String,
    val employeeName: String = "",
    val schoolName: String = "",
    val state: String = "Rajasthan",
    val district: String = "",
    val block: String = "",
    val visitDate: String = "",
    val status: VisitStatus = VisitStatus.ASSIGNED,
    // Store answers and photos json/string representations for Room
    val answersJson: String = "",
    val photosJson: String = "", // Map of Category ID -> List of image URIs/URLs
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
