package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.AppDatabase
import com.example.data.model.SyncStatus
import com.example.util.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore

    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    fun updateNetworkState() {
        _isOnline.value = isNetworkAvailable()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    suspend fun checkPendingCount() {
        withContext(Dispatchers.IO) {
            val pendingVisits = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING)
            _pendingSyncCount.value = pendingVisits.size
        }
    }

    suspend fun syncPendingData(): Boolean {
        return withContext(Dispatchers.IO) {
            updateNetworkState()
            if (!_isOnline.value) return@withContext false

            try {
                val fStore = firestore ?: return@withContext false
                val pendingVisits = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING)
                for (visit in pendingVisits) {
                    val visitMap = hashMapOf(
                        "visitId" to visit.visitId,
                        "schoolId" to visit.schoolId,
                        "employeeId" to visit.employeeId,
                        "employeeName" to visit.employeeName,
                        "schoolName" to visit.schoolName,
                        "district" to visit.district,
                        "block" to visit.block,
                        "visitDate" to visit.visitDate,
                        "status" to visit.status.name,
                        "answersJson" to visit.answersJson,
                        "photosJson" to visit.photosJson,
                        "updatedAt" to System.currentTimeMillis()
                    )

                    fStore.collection("visits")
                        .document(visit.visitId)
                        .set(visitMap)

                    db.visitDao().updateVisit(visit.copy(syncStatus = SyncStatus.SYNCED))
                }

                _pendingSyncCount.value = 0
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
