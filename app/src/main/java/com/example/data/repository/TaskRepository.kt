package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.Task
import com.example.data.model.VisitStatus
import com.example.util.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class TaskRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore

    suspend fun seedDefaultTasks() = withContext(Dispatchers.IO) {
        val sampleTask1 = Task(
            taskId = "tsk_001",
            visitId = "vst_102",
            schoolId = "sch_002",
            employeeId = "emp_001",
            employeeName = "Ramesh Kumar",
            schoolName = "Government Mahatma Gandhi English Medium School (9123842)",
            district = "JAIPUR",
            block = "AMER",
            assignedBy = "Mission Gyan Admin",
            visitDate = "14-Aug-2026",
            status = VisitStatus.ASSIGNED,
            notes = "Conduct smart class demonstration and check Mission Gyan poster installation."
        )

        val sampleTask2 = Task(
            taskId = "tsk_002",
            visitId = "vst_103",
            schoolId = "sch_003",
            employeeId = "emp_001",
            employeeName = "Ramesh Kumar",
            schoolName = "Govt Secondary School Soorsagar (7612349)",
            district = "JODHPUR",
            block = "JODHPUR URBAN",
            assignedBy = "Mission Gyan Admin",
            visitDate = "18-Aug-2026",
            status = VisitStatus.ASSIGNED,
            notes = "Interact with Principal Sir and verify SMC WhatsApp group status."
        )

        if (db.taskDao().getTaskById("tsk_001") == null) {
            db.taskDao().insertTask(sampleTask1)
        }
        if (db.taskDao().getTaskById("tsk_002") == null) {
            db.taskDao().insertTask(sampleTask2)
        }
    }

    fun getAllTasks(): Flow<List<Task>> = db.taskDao().getAllTasks()

    fun getTasksByEmployee(employeeId: String): Flow<List<Task>> = db.taskDao().getTasksByEmployee(employeeId)

    suspend fun assignTask(
        schoolId: String,
        schoolName: String,
        district: String,
        block: String,
        employeeId: String,
        employeeName: String,
        visitDate: String,
        notes: String
    ): Result<Task> = withContext(Dispatchers.IO) {
        try {
            val taskId = "tsk_" + UUID.randomUUID().toString().take(8)
            val visitId = "vst_" + UUID.randomUUID().toString().take(8)

            val task = Task(
                taskId = taskId,
                visitId = visitId,
                schoolId = schoolId,
                employeeId = employeeId,
                employeeName = employeeName,
                schoolName = schoolName,
                district = district,
                block = block,
                assignedBy = "Admin",
                visitDate = visitDate,
                status = VisitStatus.ASSIGNED,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            db.taskDao().insertTask(task)

            // Sync to Firestore
            firestore?.collection("tasks")?.document(taskId)?.set(
                mapOf(
                    "taskId" to taskId,
                    "visitId" to visitId,
                    "schoolId" to schoolId,
                    "employeeId" to employeeId,
                    "employeeName" to employeeName,
                    "schoolName" to schoolName,
                    "district" to district,
                    "block" to block,
                    "assignedBy" to "Admin",
                    "visitDate" to visitDate,
                    "status" to VisitStatus.ASSIGNED.name,
                    "notes" to notes,
                    "createdAt" to task.createdAt
                )
            )

            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(taskId: String, status: VisitStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val task = db.taskDao().getTaskById(taskId)
            if (task != null) {
                val updated = task.copy(status = status)
                db.taskDao().updateTask(updated)
                firestore?.collection("tasks")?.document(taskId)?.update("status", status.name)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
