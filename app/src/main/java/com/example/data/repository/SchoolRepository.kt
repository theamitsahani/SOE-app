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

    suspend fun seedDefaultSchools() = withContext(Dispatchers.IO) {
        val sampleSchools = listOf(
            School(
                schoolId = "sch_001",
                sr = "1",
                stateName = "Rajasthan",
                districtName = "JAIPUR",
                schoolName = "Government Senior Secondary School (8788688)",
                schoolType = "Senior Secondary",
                villageName = "Sanganer",
                principalName = "Rajesh Sharma",
                blockName = "SANGANER",
                principalMobile = "9829012345",
                visitDate = "05-Aug-2026"
            ),
            School(
                schoolId = "sch_002",
                sr = "2",
                stateName = "Rajasthan",
                districtName = "JAIPUR",
                schoolName = "Government Mahatma Gandhi English Medium School (9123842)",
                schoolType = "Secondary",
                villageName = "Amer",
                principalName = "Sunita Verma",
                blockName = "AMER",
                principalMobile = "9414056789",
                visitDate = "08-Aug-2026"
            ),
            School(
                schoolId = "sch_003",
                sr = "3",
                stateName = "Rajasthan",
                districtName = "JODHPUR",
                schoolName = "Govt Secondary School Soorsagar (7612349)",
                schoolType = "Secondary",
                villageName = "Soorsagar",
                principalName = "Mahesh Choudhary",
                blockName = "JODHPUR URBAN",
                principalMobile = "9828112233",
                visitDate = "10-Aug-2026"
            ),
            School(
                schoolId = "sch_004",
                sr = "4",
                stateName = "Rajasthan",
                districtName = "UDAIPUR",
                schoolName = "Government Higher Secondary School Girwa (6541298)",
                schoolType = "Senior Secondary",
                villageName = "Girwa",
                principalName = "Anita Rathore",
                blockName = "GIRWA",
                principalMobile = "9785123456",
                visitDate = "12-Aug-2026"
            ),
            School(
                schoolId = "sch_005",
                sr = "5",
                stateName = "Rajasthan",
                districtName = "KOTA",
                schoolName = "Govt Model School Ladpura (5432187)",
                schoolType = "Model School",
                villageName = "Ladpura",
                principalName = "Vikram Singh",
                blockName = "LADPURA",
                principalMobile = "9413234567",
                visitDate = "15-Aug-2026"
            )
        )

        for (sch in sampleSchools) {
            if (db.schoolDao().getSchoolById(sch.schoolId) == null) {
                db.schoolDao().insertSchool(sch)
            }
        }
    }

    fun getAllSchools(): Flow<List<School>> = db.schoolDao().getAllSchools()

    fun searchSchools(query: String): Flow<List<School>> = db.schoolDao().searchSchools(query)

    suspend fun getSchoolById(schoolId: String): School? = withContext(Dispatchers.IO) {
        db.schoolDao().getSchoolById(schoolId)
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
