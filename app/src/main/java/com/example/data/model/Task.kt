package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val taskId: String,
    val visitId: String,
    val schoolId: String,
    val employeeId: String,
    val employeeName: String = "",
    val schoolName: String = "",
    val state: String = "Rajasthan",
    val district: String = "",
    val block: String = "",
    val assignedBy: String = "Admin",
    val visitDate: String = "",
    val status: VisitStatus = VisitStatus.ASSIGNED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
