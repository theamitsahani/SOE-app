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

    suspend fun initializeDefaultAccounts() = withContext(Dispatchers.IO) {
        // Seed default Admin and Employee accounts into Room local cache
        val defaultAdmin = UserEntity(
            userId = "admin_001",
            name = "Mission Gyan Admin",
            email = "admin@missiongyan.org",
            mobile = "9876543210",
            role = UserRole.ADMIN.name,
            status = UserStatus.ACTIVE.name
        )

        val defaultEmployee = UserEntity(
            userId = "emp_001",
            name = "Ramesh Kumar (SOE Field Officer)",
            email = "employee@missiongyan.org",
            mobile = "9123456789",
            role = UserRole.EMPLOYEE.name,
            status = UserStatus.ACTIVE.name
        )

        if (db.userDao().getUserById("admin_001") == null) {
            db.userDao().insertUser(defaultAdmin)
        }
        if (db.userDao().getUserById("emp_001") == null) {
            db.userDao().insertUser(defaultEmployee)
        }
    }

    suspend fun login(emailOrUserId: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val input = emailOrUserId.trim()

            // 1. Check local seed/cache match for quick offline/test login
            val localUserEntity = db.userDao().getUserByEmail(input) 
                ?: db.userDao().getUserById(input) 
                ?: db.userDao().getUserByMobile(input)

            if (localUserEntity != null) {
                if (localUserEntity.status == UserStatus.INACTIVE.name) {
                    return@withContext Result.failure(Exception("This account is inactive. Please contact Admin."))
                }
                val user = User(
                    userId = localUserEntity.userId,
                    name = localUserEntity.name,
                    email = localUserEntity.email,
                    mobile = localUserEntity.mobile,
                    role = UserRole.valueOf(localUserEntity.role),
                    status = UserStatus.valueOf(localUserEntity.status)
                )
                _currentUser.value = user
                return@withContext Result.success(user)
            }

            // 2. Try Firebase Auth or demo account fallback
            if ((input == "admin@missiongyan.org" || input == "admin" || input == "9876543210") && password == "admin123") {
                val adminUser = User(
                    userId = "admin_001",
                    name = "Mission Gyan Admin",
                    email = "admin@missiongyan.org",
                    mobile = "9876543210",
                    role = UserRole.ADMIN,
                    status = UserStatus.ACTIVE
                )
                _currentUser.value = adminUser
                return@withContext Result.success(adminUser)
            } else if ((input == "employee@missiongyan.org" || input == "emp" || input == "9123456789") && password == "emp123") {
                val empUser = User(
                    userId = "emp_001",
                    name = "Ramesh Kumar (SOE Field Officer)",
                    email = "employee@missiongyan.org",
                    mobile = "9123456789",
                    role = UserRole.EMPLOYEE,
                    status = UserStatus.ACTIVE
                )
                _currentUser.value = empUser
                return@withContext Result.success(empUser)
            }

            try {
                val fAuth = firebaseAuth ?: throw Exception("Firebase Auth not initialized")
                val fStore = firestore ?: throw Exception("Firestore not initialized")

                val authTask = fAuth.signInWithEmailAndPassword(input, password)
                val authResult = com.google.android.gms.tasks.Tasks.await(authTask)
                val uid = authResult.user?.uid ?: "user_${System.currentTimeMillis()}"

                // Fetch user document from Firestore
                val docTask = fStore.collection("users").document(uid).get()
                val doc = com.google.android.gms.tasks.Tasks.await(docTask)

                val roleStr = doc.getString("role") ?: UserRole.EMPLOYEE.name
                val statusStr = doc.getString("status") ?: UserStatus.ACTIVE.name
                val name = doc.getString("name") ?: authResult.user?.displayName ?: "SOE User"
                val email = authResult.user?.email ?: input
                val mobile = doc.getString("mobile") ?: ""

                if (statusStr == UserStatus.INACTIVE.name) {
                    return@withContext Result.failure(Exception("This account is inactive."))
                }

                val user = User(
                    userId = uid,
                    name = name,
                    email = email,
                    mobile = mobile,
                    role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.EMPLOYEE },
                    status = try { UserStatus.valueOf(statusStr) } catch (e: Exception) { UserStatus.ACTIVE }
                )

                // Cache user
                db.userDao().insertUser(
                    UserEntity(
                        userId = user.userId,
                        name = user.name,
                        email = user.email,
                        mobile = user.mobile,
                        role = user.role.name,
                        status = user.status.name
                    )
                )

                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(Exception("Invalid credentials. Try admin@missiongyan.org / admin123 or employee@missiongyan.org / emp123"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        firebaseAuth?.signOut()
        _currentUser.value = null
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth?.currentUser?.updatePassword(newPassword)
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
                    role = try { UserRole.valueOf(e.role) } catch (_: Exception) { UserRole.EMPLOYEE },
                    status = try { UserStatus.valueOf(e.status) } catch (_: Exception) { UserStatus.ACTIVE }
                )
            }.filter { it.role == UserRole.EMPLOYEE }
        }
    }

    suspend fun saveEmployee(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = UserEntity(
                userId = user.userId,
                name = user.name,
                email = user.email,
                mobile = user.mobile,
                role = user.role.name,
                status = user.status.name
            )
            db.userDao().insertUser(entity)

            // Sync to Firestore
            firestore?.collection("users")?.document(user.userId)?.set(
                mapOf(
                    "userId" to user.userId,
                    "name" to user.name,
                    "email" to user.email,
                    "mobile" to user.mobile,
                    "role" to user.role.name,
                    "status" to user.status.name
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
