package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.School
import com.example.util.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SchoolRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore

    fun getAllSchools(): Flow<List<School>> = db.schoolDao().getAllSchools()

    fun searchSchools(query: String): Flow<List<School>> = db.schoolDao().searchSchools(query)

    suspend fun getSchoolById(schoolId: String): School? = withContext(Dispatchers.IO) {
        db.schoolDao().getSchoolById(schoolId)
    }

    suspend fun syncSchoolsFromFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fStore = firestore ?: return@withContext Result.failure(Exception("Firestore not initialized"))
            val snapshotTask = fStore.collection("schools").get()
            val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

            val schools = snapshot.documents.mapNotNull { doc ->
                val schoolId = doc.getString("schoolId") ?: doc.id
                val schoolName = doc.getString("schoolName") ?: ""
                if (schoolName.isBlank()) return@mapNotNull null

                val stateName = doc.getString("stateName") ?: doc.getString("state") ?: "Rajasthan"
                val districtName = doc.getString("districtName") ?: doc.getString("district") ?: ""
                val schoolType = doc.getString("schoolType") ?: doc.getString("type") ?: ""
                val villageName = doc.getString("villageName") ?: doc.getString("village") ?: ""
                val principalMobile = doc.getString("principalMobile") ?: doc.getString("mobile") ?: doc.getString("principalPhone") ?: ""
                val visitDate = doc.getString("visitDate") ?: doc.getString("originalVisitDate") ?: ""

                School(
                    schoolId = schoolId,
                    sr = doc.getString("sr") ?: "",
                    stateName = stateName,
                    districtName = districtName,
                    schoolName = schoolName,
                    schoolType = schoolType,
                    villageName = villageName,
                    principalName = doc.getString("principalName") ?: "",
                    blockName = doc.getString("blockName") ?: doc.getString("block") ?: "",
                    principalMobile = principalMobile,
                    visitDate = visitDate,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }

            if (schools.isNotEmpty()) {
                db.schoolDao().insertSchools(schools)
            }
            Result.success(schools.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importSchools(schools: List<School>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            db.schoolDao().insertSchools(schools)

            val fStore = firestore
            if (fStore != null) {
                // Sync to Firestore
                for (sch in schools) {
                    val task = fStore.collection("schools").document(sch.schoolId).set(
                        mapOf(
                            "schoolId" to sch.schoolId,
                            "state" to sch.stateName,
                            "stateName" to sch.stateName,
                            "district" to sch.districtName,
                            "districtName" to sch.districtName,
                            "schoolName" to sch.schoolName,
                            "type" to sch.schoolType,
                            "schoolType" to sch.schoolType,
                            "village" to sch.villageName,
                            "villageName" to sch.villageName,
                            "principalName" to sch.principalName,
                            "block" to sch.blockName,
                            "blockName" to sch.blockName,
                            "mobile" to sch.principalMobile,
                            "principalMobile" to sch.principalMobile,
                            "visitDate" to sch.visitDate,
                            "sr" to sch.sr,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    com.google.android.gms.tasks.Tasks.await(task)
                }
            }

            Result.success(schools.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing school record when an employee or admin modifies information.
     * Prevents duplicate creation!
     */
    suspend fun updateSchoolRecord(school: School): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = school.copy(updatedAt = System.currentTimeMillis())
            db.schoolDao().updateSchool(updated)

            val fStore = firestore
            if (fStore != null) {
                // Sync update to Firestore
                val task = fStore.collection("schools").document(school.schoolId).set(
                    mapOf(
                        "schoolId" to updated.schoolId,
                        "state" to updated.stateName,
                        "stateName" to updated.stateName,
                        "district" to updated.districtName,
                        "districtName" to updated.districtName,
                        "schoolName" to updated.schoolName,
                        "type" to updated.schoolType,
                        "schoolType" to updated.schoolType,
                        "village" to updated.villageName,
                        "villageName" to updated.villageName,
                        "principalName" to updated.principalName,
                        "block" to updated.blockName,
                        "blockName" to updated.blockName,
                        "mobile" to updated.principalMobile,
                        "principalMobile" to updated.principalMobile,
                        "visitDate" to updated.visitDate,
                        "updatedAt" to updated.updatedAt
                    )
                )
                com.google.android.gms.tasks.Tasks.await(task)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a school record by its ID from local database and Firestore.
     */
    suspend fun deleteSchool(schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.schoolDao().deleteSchoolById(schoolId)
            firestore?.collection("schools")?.document(schoolId)?.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
