package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.SyncStatus
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.util.FirebaseUtils
import com.example.util.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VisitRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore
    private val syncManager = SyncManager(context)

    fun getAllVisits(): Flow<List<Visit>> = db.visitDao().getAllVisits()

    fun getVisitsBySchool(schoolId: String): Flow<List<Visit>> = db.visitDao().getVisitsBySchool(schoolId)

    fun getVisitsByEmployee(employeeId: String): Flow<List<Visit>> = db.visitDao().getVisitsByEmployee(employeeId)

    suspend fun getVisitById(visitId: String): Visit? = withContext(Dispatchers.IO) {
        db.visitDao().getVisitById(visitId)
    }

    suspend fun syncVisitsFromFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fStore = firestore ?: return@withContext Result.failure(Exception("Firestore not initialized"))
            val snapshotTask = fStore.collection("visits").get()
            val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

            val visits = snapshot.documents.mapNotNull { doc ->
                val visitId = doc.getString("visitId") ?: doc.id
                val schoolId = doc.getString("schoolId") ?: ""
                val employeeId = doc.getString("employeeId") ?: ""
                val schoolName = doc.getString("schoolName") ?: ""
                if (schoolName.isBlank()) return@mapNotNull null

                val statusStr = doc.getString("status") ?: VisitStatus.SUBMITTED.name
                val status = try { VisitStatus.valueOf(statusStr) } catch (e: Exception) { VisitStatus.SUBMITTED }

                Visit(
                    visitId = visitId,
                    schoolId = schoolId,
                    employeeId = employeeId,
                    employeeName = doc.getString("employeeName") ?: "",
                    schoolName = schoolName,
                    district = doc.getString("district") ?: "",
                    block = doc.getString("block") ?: "",
                    visitDate = doc.getString("visitDate") ?: "",
                    status = status,
                    answersJson = doc.getString("answersJson") ?: "{}",
                    photosJson = doc.getString("photosJson") ?: "{}",
                    syncStatus = SyncStatus.SYNCED,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }

            if (visits.isNotEmpty()) {
                db.visitDao().insertVisits(visits)
            }
            Result.success(visits.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits a visit report safely with offline persistence and slow network protection.
     */
    suspend fun submitVisit(visit: Visit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isOnline = syncManager.isNetworkAvailable()
            val initialSyncStatus = if (isOnline) SyncStatus.PENDING else SyncStatus.PENDING

            val finalVisit = visit.copy(
                status = VisitStatus.SUBMITTED,
                syncStatus = initialSyncStatus,
                updatedAt = System.currentTimeMillis()
            )

            // Step 1: Save to Room DB locally first (ensures 100% offline durability)
            db.visitDao().insertVisit(finalVisit)
            
            // Step 2: Mark all assigned tasks for this school as SUBMITTED so co-officers see it as completed
            db.taskDao().markTasksSubmittedForSchool(finalVisit.schoolId)

            // Step 3: If online, attempt immediate background upload with timeout protection
            if (isOnline) {
                val uploaded = syncManager.uploadSingleVisitToServer(finalVisit)
                if (uploaded) {
                    db.visitDao().updateVisit(finalVisit.copy(syncStatus = SyncStatus.SYNCED))
                } else {
                    // Stays PENDING; SyncManager auto-sync will upload as soon as network stabilizes
                    db.visitDao().updateVisit(finalVisit.copy(syncStatus = SyncStatus.PENDING))
                }
            }

            syncManager.checkPendingCount()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            // Even on unexpected exception, try local save fallback
            try {
                db.visitDao().insertVisit(visit.copy(status = VisitStatus.SUBMITTED, syncStatus = SyncStatus.PENDING, updatedAt = System.currentTimeMillis()))
                syncManager.checkPendingCount()
                Result.success(Unit)
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
        }
    }
}
