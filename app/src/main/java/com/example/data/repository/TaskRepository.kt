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

    fun getAllTasks(): Flow<List<Task>> = db.taskDao().getAllTasks()

    fun getTasksByEmployee(employeeId: String): Flow<List<Task>> = db.taskDao().getTasksByEmployee(employeeId)

    suspend fun syncTasksFromFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fStore = firestore ?: return@withContext Result.failure(Exception("Firestore not initialized"))
            val snapshotTask = fStore.collection("tasks").get()
            val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

            val tasks = snapshot.documents.mapNotNull { doc ->
                val taskId = doc.getString("taskId") ?: doc.id
                val schoolId = doc.getString("schoolId") ?: ""
                val employeeId = doc.getString("employeeId") ?: ""
                val schoolName = doc.getString("schoolName") ?: ""
                if (schoolName.isBlank()) return@mapNotNull null

                val statusStr = doc.getString("status") ?: VisitStatus.ASSIGNED.name
                val status = try { VisitStatus.valueOf(statusStr) } catch (e: Exception) { VisitStatus.ASSIGNED }

                Task(
                    taskId = taskId,
                    visitId = doc.getString("visitId") ?: "",
                    schoolId = schoolId,
                    employeeId = employeeId,
                    employeeName = doc.getString("employeeName") ?: "",
                    schoolName = schoolName,
                    district = doc.getString("district") ?: "",
                    block = doc.getString("block") ?: "",
                    assignedBy = doc.getString("assignedBy") ?: "Admin",
                    visitDate = doc.getString("visitDate") ?: "",
                    status = status,
                    notes = doc.getString("notes") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }

            if (tasks.isNotEmpty()) {
                db.taskDao().insertTasks(tasks)
            }
            Result.success(tasks.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
