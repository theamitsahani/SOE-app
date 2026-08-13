package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.PhotoCategory
import com.example.data.model.SyncStatus
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.data.model.VisitStatus
import com.example.util.FirebaseUtils
import com.example.util.SyncManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VisitRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore
    private val syncManager = SyncManager(context)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    suspend fun seedDefaultVisits() = withContext(Dispatchers.IO) {
        val answers1 = VisitAnswers(
            q1_soeName = "Ramesh Kumar",
            q2_visitDate = "05-Aug-2026",
            q3_schoolName = "Government Senior Secondary School (8788688)",
            q4_udiseCode = "08120304501",
            q5_district = "JAIPUR",
            q6_block = "SANGANER",
            q7_principalName = "Rajesh Sharma",
            q8_principalMobile = "9829012345",
            q9_metPrincipal = "हाँ",
            q10_missionGyanAwareness = "हाँ",
            q11_studentCount = "450",
            q12_schoolResponse = "बहुत अच्छी",
            q13_bciContactDetails = "Amit Kumar - 9876543210",
            q14_whatsappGroupAdded = "हाँ",
            q15_posterInstalled = "हाँ",
            q16_keyObservations = "Smart board active, students enthusiastic about Mission Gyan video content.",
            q17_problemsOrAssistance = "Minor internet connectivity lag during afternoon peak hours.",
            q18_followupRequired = "नहीं",
            q20_finalRemarks = "Excellent cooperation from school staff.",
            q21_smartClassStatus = "बहुत अच्छी"
        )

        val answersAdapter = moshi.adapter(VisitAnswers::class.java)
        val photosMap = mapOf(
            PhotoCategory.SCHOOL_PHOTO.categoryId to listOf("https://images.unsplash.com/photo-1580582932707-520aed937b7b?w=600"),
            PhotoCategory.EXPLAINING_APP.categoryId to listOf("https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=600"),
            PhotoCategory.STUDENTS_SMART_BOARD.categoryId to listOf("https://images.unsplash.com/photo-1509062522246-3755977927d7?w=600"),
            PhotoCategory.PRINCIPAL_PHOTO.categoryId to listOf("https://images.unsplash.com/photo-1577896851231-70ef18881754?w=600"),
            PhotoCategory.LETTER_PHOTO.categoryId to listOf("https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=600"),
            PhotoCategory.OTHER_PHOTOS.categoryId to listOf("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600")
        )

        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
        val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

        val sampleVisit = Visit(
            visitId = "vst_101",
            schoolId = "sch_001",
            employeeId = "emp_001",
            employeeName = "Ramesh Kumar",
            schoolName = "Government Senior Secondary School (8788688)",
            district = "JAIPUR",
            block = "SANGANER",
            visitDate = "05-Aug-2026",
            status = VisitStatus.SUBMITTED,
            answersJson = answersAdapter.toJson(answers1),
            photosJson = photosAdapter.toJson(photosMap),
            syncStatus = SyncStatus.SYNCED
        )

        if (db.visitDao().getVisitById("vst_101") == null) {
            db.visitDao().insertVisit(sampleVisit)
        }
    }

    fun getAllVisits(): Flow<List<Visit>> = db.visitDao().getAllVisits()

    fun getVisitsBySchool(schoolId: String): Flow<List<Visit>> = db.visitDao().getVisitsBySchool(schoolId)

    fun getVisitsByEmployee(employeeId: String): Flow<List<Visit>> = db.visitDao().getVisitsByEmployee(employeeId)

    suspend fun getVisitById(visitId: String): Visit? = withContext(Dispatchers.IO) {
        db.visitDao().getVisitById(visitId)
    }

    /**
     * Submits a visit report safely with duplicate submission protection.
     */
    suspend fun submitVisit(visit: Visit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            syncManager.updateNetworkState()
            val isOnline = syncManager.isOnline.value
            val currentSyncStatus = if (isOnline) SyncStatus.SYNCED else SyncStatus.PENDING

            val finalVisit = visit.copy(
                status = VisitStatus.SUBMITTED,
                syncStatus = currentSyncStatus,
                updatedAt = System.currentTimeMillis()
            )

            // Save to Room DB locally first (ensures offline durability)
            db.visitDao().insertVisit(finalVisit)

            if (isOnline) {
                try {
                    firestore?.collection("visits")
                        ?.document(finalVisit.visitId)
                        ?.set(
                            mapOf(
                                "visitId" to finalVisit.visitId,
                                "schoolId" to finalVisit.schoolId,
                                "employeeId" to finalVisit.employeeId,
                                "employeeName" to finalVisit.employeeName,
                                "schoolName" to finalVisit.schoolName,
                                "district" to finalVisit.district,
                                "block" to finalVisit.block,
                                "visitDate" to finalVisit.visitDate,
                                "status" to finalVisit.status.name,
                                "answersJson" to finalVisit.answersJson,
                                "photosJson" to finalVisit.photosJson,
                                "updatedAt" to finalVisit.updatedAt
                            )
                        )
                } catch (e: Exception) {
                    // Downgrade syncStatus to PENDING if network fails mid-upload
                    db.visitDao().updateVisit(finalVisit.copy(syncStatus = SyncStatus.PENDING))
                }
            }

            syncManager.checkPendingCount()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
