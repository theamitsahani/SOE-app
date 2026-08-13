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
                district = "JAIPUR",
                schoolName = "Government Senior Secondary School (8788688)",
                referenceCode = "8788688",
                type = "Senior Secondary",
                village = "Sanganer",
                principalName = "Rajesh Sharma",
                block = "SANGANER",
                mobile = "9829012345",
                originalVisitDate = "05-Aug-2026"
            ),
            School(
                schoolId = "sch_002",
                sr = "2",
                district = "JAIPUR",
                schoolName = "Government Mahatma Gandhi English Medium School (9123842)",
                referenceCode = "9123842",
                type = "Secondary",
                village = "Amer",
                principalName = "Sunita Verma",
                block = "AMER",
                mobile = "9414056789",
                originalVisitDate = "08-Aug-2026"
            ),
            School(
                schoolId = "sch_003",
                sr = "3",
                district = "JODHPUR",
                schoolName = "Govt Secondary School Soorsagar (7612349)",
                referenceCode = "7612349",
                type = "Secondary",
                village = "Soorsagar",
                principalName = "Mahesh Choudhary",
                block = "JODHPUR URBAN",
                mobile = "9828112233",
                originalVisitDate = "10-Aug-2026"
            ),
            School(
                schoolId = "sch_004",
                sr = "4",
                district = "UDAIPUR",
                schoolName = "Government Higher Secondary School Girwa (6541298)",
                referenceCode = "6541298",
                type = "Senior Secondary",
                village = "Girwa",
                principalName = "Anita Rathore",
                block = "GIRWA",
                mobile = "9785123456",
                originalVisitDate = "12-Aug-2026"
            ),
            School(
                schoolId = "sch_005",
                sr = "5",
                district = "KOTA",
                schoolName = "Govt Model School Ladpura (5432187)",
                referenceCode = "5432187",
                type = "Model School",
                village = "Ladpura",
                principalName = "Vikram Singh",
                block = "LADPURA",
                mobile = "9413234567",
                originalVisitDate = "15-Aug-2026"
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
                        "sr" to sch.sr,
                        "district" to sch.district,
                        "schoolName" to sch.schoolName,
                        "referenceCode" to sch.referenceCode,
                        "type" to sch.type,
                        "village" to sch.village,
                        "principalName" to sch.principalName,
                        "block" to sch.block,
                        "mobile" to sch.mobile,
                        "originalVisitDate" to sch.originalVisitDate,
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
            firestore?.collection("schools")?.document(school.schoolId)?.update(
                mapOf(
                    "schoolName" to updated.schoolName,
                    "district" to updated.district,
                    "block" to updated.block,
                    "principalName" to updated.principalName,
                    "mobile" to updated.mobile,
                    "referenceCode" to updated.referenceCode,
                    "updatedAt" to updated.updatedAt
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
