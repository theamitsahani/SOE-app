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

                School(
                    schoolId = schoolId,
                    sr = doc.getString("sr") ?: "",
                    stateName = doc.getString("stateName") ?: "Rajasthan",
                    districtName = doc.getString("districtName") ?: "",
                    schoolName = schoolName,
                    schoolType = doc.getString("schoolType") ?: "",
                    villageName = doc.getString("villageName") ?: "",
                    principalName = doc.getString("principalName") ?: "",
                    blockName = doc.getString("blockName") ?: "",
                    principalMobile = doc.getString("principalMobile") ?: "",
                    visitDate = doc.getString("visitDate") ?: "",
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

            // Sync to Firestore
            for (sch in schools) {
                firestore?.collection("schools")?.document(sch.schoolId)?.set(
                    mapOf(
                        "schoolId" to sch.schoolId,
                        "stateName" to sch.stateName,
                        "districtName" to sch.districtName,
                        "schoolName" to sch.schoolName,
                        "schoolType" to sch.schoolType,
                        "villageName" to sch.villageName,
                        "principalName" to sch.principalName,
                        "blockName" to sch.blockName,
                        "principalMobile" to sch.principalMobile,
                        "visitDate" to sch.visitDate,
                        "sr" to sch.sr,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
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

            // Sync update to Firestore
            firestore?.collection("schools")?.document(school.schoolId)?.set(
                mapOf(
                    "schoolId" to updated.schoolId,
                    "stateName" to updated.stateName,
                    "districtName" to updated.districtName,
                    "schoolName" to updated.schoolName,
                    "schoolType" to updated.schoolType,
                    "villageName" to updated.villageName,
                    "principalName" to updated.principalName,
                    "blockName" to updated.blockName,
                    "principalMobile" to updated.principalMobile,
                    "visitDate" to updated.visitDate,
                    "updatedAt" to updated.updatedAt
                )
            )

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
