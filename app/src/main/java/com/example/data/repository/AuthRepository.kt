package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.UserEntity
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.util.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firebaseAuth get() = FirebaseUtils.auth
    private val firestore get() = FirebaseUtils.firestore

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun checkCurrentSession(): User? = withContext(Dispatchers.IO) {
        try {
            val fAuth = firebaseAuth ?: return@withContext null
            val currentFbUser = fAuth.currentUser ?: return@withContext null
            val uid = currentFbUser.uid

            // Check local cache first
            val localUser = db.userDao().getUserById(uid)
            if (localUser != null && localUser.status != UserStatus.INACTIVE.name) {
                val user = User(
                    userId = localUser.userId,
                    name = localUser.name,
                    email = localUser.email,
                    mobile = localUser.mobile,
                    state = localUser.state,
                    district = localUser.district,
                    role = try { UserRole.valueOf(localUser.role) } catch (e: Exception) { UserRole.EMPLOYEE },
                    status = try { UserStatus.valueOf(localUser.status) } catch (e: Exception) { UserStatus.ACTIVE }
                )
                _currentUser.value = user
                return@withContext user
            }

            // Otherwise fetch from Firestore
            val fStore = firestore ?: return@withContext null
            val docTask = fStore.collection("users").document(uid).get()
            val doc = com.google.android.gms.tasks.Tasks.await(docTask)
            if (doc.exists()) {
                val roleStr = doc.getString("role") ?: UserRole.EMPLOYEE.name
                val statusStr = doc.getString("status") ?: UserStatus.ACTIVE.name
                val name = doc.getString("name") ?: currentFbUser.displayName ?: "User"
                val email = doc.getString("email") ?: currentFbUser.email ?: ""
                val mobile = doc.getString("mobile") ?: ""
                val state = doc.getString("state") ?: "Rajasthan"
                val district = doc.getString("district") ?: ""

                val user = User(
                    userId = uid,
                    name = name,
                    email = email,
                    mobile = mobile,
                    state = state,
                    district = district,
                    role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.EMPLOYEE },
                    status = try { UserStatus.valueOf(statusStr) } catch (e: Exception) { UserStatus.ACTIVE }
                )
                if (user.status == UserStatus.ACTIVE) {
                    db.userDao().insertUser(
                        UserEntity(
                            userId = user.userId,
                            name = user.name,
                            email = user.email,
                            mobile = user.mobile,
                            state = user.state,
                            district = user.district,
                            role = user.role.name,
                            status = user.status.name
                        )
                    )
                    _currentUser.value = user
                    return@withContext user
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun login(emailOrUserId: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val input = emailOrUserId.trim()
            if (input.isBlank() || password.isBlank()) {
                return@withContext Result.failure(Exception("Please enter your email and password."))
            }

            val fAuth = firebaseAuth ?: return@withContext Result.failure(Exception("Firebase Auth is not available."))
            val fStore = firestore

            // Perform real Firebase Authentication
            val authTask = fAuth.signInWithEmailAndPassword(input, password)
            val authResult = com.google.android.gms.tasks.Tasks.await(authTask)
            val fbUser = authResult.user ?: return@withContext Result.failure(Exception("User not found."))
            val uid = fbUser.uid
            val userEmail = fbUser.email ?: input

            // Fetch user profile from Firestore if online/available
            var role = UserRole.EMPLOYEE
            var status = UserStatus.ACTIVE
            var name = fbUser.displayName ?: "Field Officer"
            var mobile = ""
            var state = "Rajasthan"
            var district = ""

            if (fStore != null) {
                try {
                    val docTask = fStore.collection("users").document(uid).get()
                    val doc = com.google.android.gms.tasks.Tasks.await(docTask)

                    if (doc.exists()) {
                        val roleStr = doc.getString("role") ?: UserRole.EMPLOYEE.name
                        val statusStr = doc.getString("status") ?: UserStatus.ACTIVE.name
                        role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.EMPLOYEE }
                        status = try { UserStatus.valueOf(statusStr) } catch (e: Exception) { UserStatus.ACTIVE }
                        name = doc.getString("name") ?: name
                        mobile = doc.getString("mobile") ?: ""
                        state = doc.getString("state") ?: "Rajasthan"
                        district = doc.getString("district") ?: ""
                    } else {
                        // If document doesn't exist yet (e.g. Firebase Console Admin setup):
                        val isAdminEmail = userEmail.contains("admin", ignoreCase = true)
                        role = if (isAdminEmail) UserRole.ADMIN else UserRole.EMPLOYEE
                        name = if (isAdminEmail) "Admin" else (fbUser.displayName ?: "Field Officer")
                        
                        fStore.collection("users").document(uid).set(
                            mapOf(
                                "userId" to uid,
                                "name" to name,
                                "email" to userEmail,
                                "mobile" to mobile,
                                "state" to state,
                                "district" to district,
                                "role" to role.name,
                                "status" to status.name,
                                "createdAt" to System.currentTimeMillis()
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Fallback to local cached profile if offline
                    val local = db.userDao().getUserById(uid)
                    if (local != null) {
                        role = try { UserRole.valueOf(local.role) } catch (ex: Exception) { UserRole.EMPLOYEE }
                        status = try { UserStatus.valueOf(local.status) } catch (ex: Exception) { UserStatus.ACTIVE }
                        name = local.name
                        mobile = local.mobile
                        state = local.state
                        district = local.district
                    }
                }
            }

            if (status == UserStatus.INACTIVE) {
                fAuth.signOut()
                return@withContext Result.failure(Exception("This account is inactive. Please contact your Admin."))
            }

            val authenticatedUser = User(
                userId = uid,
                name = name,
                email = userEmail,
                mobile = mobile,
                state = state,
                district = district,
                role = role,
                status = status
            )

            // Cache in local database without password
            db.userDao().insertUser(
                UserEntity(
                    userId = authenticatedUser.userId,
                    name = authenticatedUser.name,
                    email = authenticatedUser.email,
                    mobile = authenticatedUser.mobile,
                    state = authenticatedUser.state,
                    district = authenticatedUser.district,
                    role = authenticatedUser.role.name,
                    status = authenticatedUser.status.name
                )
            )

            _currentUser.value = authenticatedUser
            Result.success(authenticatedUser)
        } catch (e: Exception) {
            val message = e.localizedMessage ?: "Authentication failed"
            Result.failure(Exception(message))
        }
    }

    fun logout() {
        firebaseAuth?.signOut()
        _currentUser.value = null
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = firebaseAuth?.currentUser ?: throw Exception("Not authenticated")
            val task = currentUser.updatePassword(newPassword)
            com.google.android.gms.tasks.Tasks.await(task)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllEmployees(): Flow<List<User>> {
        return db.userDao().getAllUsers().map { entities ->
            entities.map { e ->
                User(
                    userId = e.userId,
                    name = e.name,
                    email = e.email,
                    mobile = e.mobile,
                    state = e.state,
                    district = e.district,
                    role = try { UserRole.valueOf(e.role) } catch (_: Exception) { UserRole.EMPLOYEE },
                    status = try { UserStatus.valueOf(e.status) } catch (_: Exception) { UserStatus.ACTIVE }
                )
            }.filter { it.role == UserRole.EMPLOYEE }
        }
    }

    suspend fun syncEmployeesFromFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fStore = firestore ?: return@withContext Result.failure(Exception("Firestore not initialized"))
            val snapshotTask = fStore.collection("users").get()
            val snapshot = com.google.android.gms.tasks.Tasks.await(snapshotTask)

            val users = snapshot.documents.mapNotNull { doc ->
                val userId = doc.getString("userId") ?: doc.id
                val name = doc.getString("name") ?: ""
                val email = doc.getString("email") ?: ""
                val mobile = doc.getString("mobile") ?: ""
                val state = doc.getString("state") ?: "Rajasthan"
                val district = doc.getString("district") ?: ""
                val roleStr = doc.getString("role") ?: UserRole.EMPLOYEE.name
                val statusStr = doc.getString("status") ?: UserStatus.ACTIVE.name

                UserEntity(
                    userId = userId,
                    name = name,
                    email = email,
                    mobile = mobile,
                    state = state,
                    district = district,
                    role = roleStr,
                    status = statusStr
                )
            }

            if (users.isNotEmpty()) {
                db.userDao().insertUsers(users)
            }
            Result.success(users.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveEmployee(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = UserEntity(
                userId = user.userId,
                name = user.name,
                email = user.email,
                mobile = user.mobile,
                state = user.state,
                district = user.district,
                role = user.role.name,
                status = user.status.name
            )
            db.userDao().insertUser(entity)

            // Sync to Firestore without plain passwords
            firestore?.collection("users")?.document(user.userId)?.set(
                mapOf(
                    "userId" to user.userId,
                    "name" to user.name,
                    "email" to user.email,
                    "mobile" to user.mobile,
                    "state" to user.state,
                    "district" to user.district,
                    "role" to user.role.name,
                    "status" to user.status.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
