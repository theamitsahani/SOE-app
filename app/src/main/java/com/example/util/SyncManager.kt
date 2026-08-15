package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.AppDatabase
import com.example.data.model.SyncStatus
import com.example.data.model.Visit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SyncManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore get() = FirebaseUtils.firestore
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        registerNetworkCallback()
        syncScope.launch {
            checkPendingCount()
            if (isNetworkAvailable()) {
                syncPendingData()
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            connectivityManager.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    // Auto-sync as soon as network is detected!
                    syncScope.launch {
                        checkPendingCount()
                        syncPendingData()
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = isNetworkAvailable()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    _isOnline.value = hasInternet
                    if (hasInternet) {
                        syncScope.launch {
                            checkPendingCount()
                            syncPendingData()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateNetworkState() {
        _isOnline.value = isNetworkAvailable()
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
            actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkPendingCount() {
        withContext(Dispatchers.IO) {
            val pendingVisits = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING)
            _pendingSyncCount.value = pendingVisits.size
        }
    }

    /**
     * Uploads all pending offline/slow-network saved visits to the server safely.
     */
    suspend fun syncPendingData(): Boolean = withContext(Dispatchers.IO) {
        updateNetworkState()
        if (!_isOnline.value) return@withContext false
        if (_isSyncing.value) return@withContext false

        _isSyncing.value = true
        try {
            val pendingVisits = db.visitDao().getVisitsBySyncStatus(SyncStatus.PENDING)
            if (pendingVisits.isEmpty()) {
                _pendingSyncCount.value = 0
                _isSyncing.value = false
                return@withContext true
            }

            var anySuccess = false
            for (visit in pendingVisits) {
                val uploadSuccess = uploadSingleVisitToServer(visit)
                if (uploadSuccess) {
                    db.visitDao().updateVisit(visit.copy(syncStatus = SyncStatus.SYNCED))
                    anySuccess = true
                }
            }

            checkPendingCount()
            _isSyncing.value = false
            anySuccess
        } catch (e: Exception) {
            e.printStackTrace()
            _isSyncing.value = false
            false
        }
    }

    suspend fun uploadSingleVisitToServer(visit: Visit): Boolean = withContext(Dispatchers.IO) {
        val fStore = firestore ?: return@withContext false
        try {
            // Set 5-second timeout per document to handle slow network gracefully
            val success = withTimeoutOrNull(5000L) {
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
                    "updatedAt" to visit.updatedAt
                )

                val setTask = fStore.collection("visits")
                    .document(visit.visitId)
                    .set(visitMap)
                com.google.android.gms.tasks.Tasks.await(setTask)
                true
            }
            success == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
