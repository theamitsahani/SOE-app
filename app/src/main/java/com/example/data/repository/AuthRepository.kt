package com.example.data.repository

import android.content.Context
import android.util.Log
import android.util.Patterns
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
            val userEmail = currentFbUser.email ?: ""

            // 1. Try to fetch the latest role & status from Firestore
            val fStore = firestore
            if (fStore != null) {
                try {
                    var userDoc: com.google.firebase.firestore.DocumentSnapshot? = null
                    val docTask = fStore.collection("users").document(uid).get()
                    val doc = com.google.android.gms.tasks.Tasks.await(docTask)
                    if (doc.exists()) {
                        userDoc = doc
                    } else {
                        val queryTask = fStore.collection("users").whereEqualTo("email", userEmail).limit(1).get()
                        val querySnap = com.google.android.gms.tasks.Tasks.await(queryTask)
                        if (!querySnap.isEmpty) {
                            userDoc = querySnap.documents.firstOrNull()
                        }
                    }

                    if (userDoc != null && userDoc.exists()) {
                        val statusStr = userDoc.getString("status")?.trim()?.uppercase() ?: UserStatus.ACTIVE.name
                        if (statusStr == "INACTIVE") {
                            fAuth.signOut()
                            _currentUser.value = null
                            return@withContext null
                        }

                        val rawRole = userDoc.getString("role")?.trim()?.uppercase()
                        val role = when (rawRole) {
                            "ADMIN" -> UserRole.ADMIN
                            "EMPLOYEE" -> UserRole.EMPLOYEE
                            else -> null
                        }

                        if (role == null) {
                            fAuth.signOut()
                            _currentUser.value = null
                            return@withContext null
                        }

                        val name = userDoc.getString("name")?.takeIf { it.isNotBlank() } ?: currentFbUser.displayName ?: (if (role == UserRole.ADMIN) "Admin" else "Field Officer")
                        val email = userDoc.getString("email") ?: userEmail
                        val mobile = userDoc.getString("mobile") ?: ""
                        val state = userDoc.getString("state") ?: "Rajasthan"
                        val district = userDoc.getString("district") ?: ""

                        val user = User(
                            userId = uid,
                            name = name,
                            email = email,
                            mobile = mobile,
                            state = state,
                            district = district,
                            role = role,
                            status = UserStatus.ACTIVE
                        )

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
                    } else {
                        // User document does not exist in Firestore
                        fAuth.signOut()
                        _currentUser.value = null
                        return@withContext null
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Failed to fetch session user profile from Firestore, checking local cache", e)
                }
            }

            // 2. Fallback to local cache if offline
            val localUser = db.userDao().getUserById(uid)
            if (localUser != null && localUser.status.uppercase() != "INACTIVE") {
                val role = when (localUser.role.uppercase()) {
                    "ADMIN" -> UserRole.ADMIN
                    "EMPLOYEE" -> UserRole.EMPLOYEE
                    else -> null
                }
                if (role != null) {
                    val user = User(
                        userId = localUser.userId,
                        name = localUser.name,
                        email = localUser.email,
                        mobile = localUser.mobile,
                        state = localUser.state,
                        district = localUser.district,
                        role = role,
                        status = UserStatus.ACTIVE
                    )
                    _currentUser.value = user
                    return@withContext user
                }
            }

            fAuth.signOut()
            _currentUser.value = null
            null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking current session", e)
            firebaseAuth?.signOut()
            _currentUser.value = null
            null
        }
    }

    suspend fun login(emailOrUserId: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val input = emailOrUserId.trim()
            if (input.isBlank()) {
                return@withContext Result.failure(Exception("Please enter your email address."))
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                return@withContext Result.failure(Exception("Please enter a valid email address."))
            }
            if (password.isBlank()) {
                return@withContext Result.failure(Exception("Please enter your password."))
            }

            val fAuth = firebaseAuth ?: return@withContext Result.failure(Exception("Internet connection unavailable. Please try again."))
            val fStore = firestore ?: return@withContext Result.failure(Exception("Internet connection unavailable. Please try again."))

            // 1. Perform Firebase Authentication
            val authTask = fAuth.signInWithEmailAndPassword(input, password)
            val authResult = com.google.android.gms.tasks.Tasks.await(authTask)
            val fbUser = authResult.user ?: run {
                fAuth.signOut()
                return@withContext Result.failure(Exception("User profile not found. Please contact administrator."))
            }
            val uid = fbUser.uid
            val userEmail = fbUser.email ?: input

            // 2. Fetch users/{UID} document from Firestore
            var userDoc: com.google.firebase.firestore.DocumentSnapshot? = null
            try {
                val docTask = fStore.collection("users").document(uid).get()
                val doc = com.google.android.gms.tasks.Tasks.await(docTask)
                if (doc.exists()) {
                    userDoc = doc
                } else {
                    val queryTask = fStore.collection("users").whereEqualTo("email", userEmail).limit(1).get()
                    val querySnap = com.google.android.gms.tasks.Tasks.await(queryTask)
                    if (!querySnap.isEmpty) {
                        userDoc = querySnap.documents.firstOrNull()
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error reading user document from Firestore", e)
                fAuth.signOut()
                return@withContext Result.failure(Exception(mapAuthErrorToUserMessage(e)))
            }

            if (userDoc == null || !userDoc.exists()) {
                fAuth.signOut()
                return@withContext Result.failure(Exception("User profile not found. Please contact administrator."))
            }

            // 3. Check status field
            val statusStr = userDoc.getString("status")?.trim()?.uppercase() ?: UserStatus.ACTIVE.name
            if (statusStr == "INACTIVE") {
                fAuth.signOut()
                return@withContext Result.failure(Exception("Your account is inactive. Please contact administrator."))
            }

            // 4. Check role field
            val rawRole = userDoc.getString("role")?.trim()?.uppercase()
            val role: UserRole = when (rawRole) {
                "ADMIN" -> UserRole.ADMIN
                "EMPLOYEE" -> UserRole.EMPLOYEE
                else -> {
                    fAuth.signOut()
                    return@withContext Result.failure(Exception("User profile not found. Please contact administrator."))
                }
            }

            val name = userDoc.getString("name")?.takeIf { it.isNotBlank() } ?: fbUser.displayName ?: (if (role == UserRole.ADMIN) "Admin" else "Field Officer")
            val mobile = userDoc.getString("mobile") ?: ""
            val state = userDoc.getString("state") ?: "Rajasthan"
            val district = userDoc.getString("district") ?: ""

            val authenticatedUser = User(
                userId = uid,
                name = name,
                email = userEmail,
                mobile = mobile,
                state = state,
                district = district,
                role = role,
                status = UserStatus.ACTIVE
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
        } catch (e: Throwable) {
            firebaseAuth?.signOut()
            val userFriendlyMessage = mapAuthErrorToUserMessage(e)
            Result.failure(Exception(userFriendlyMessage))
        }
    }

    private fun mapAuthErrorToUserMessage(e: Throwable): String {
        // Log the complete technical error and stack trace ONLY in Logcat for debugging
        Log.e("AuthRepository", "Authentication failure encountered", e)

        val rootCause = e.cause ?: e
        val rawMessage = (rootCause.message ?: "").lowercase()
        val className = rootCause::class.java.simpleName

        return when {
            // Explicit business rules
            rawMessage.contains("your account is inactive") || rawMessage.contains("account is inactive") -> {
                "Your account is inactive. Please contact administrator."
            }
            rawMessage.contains("user profile not found") -> {
                "User profile not found. Please contact administrator."
            }
            rawMessage.contains("please enter a valid email") -> {
                "Please enter a valid email address."
            }
            rawMessage.contains("please enter your email") -> {
                "Please enter your email address."
            }
            rawMessage.contains("please enter your password") -> {
                "Please enter your password."
            }

            // Invalid Email Format from Firebase
            rawMessage.contains("badly formatted") ||
            rawMessage.contains("invalid email") ||
            rawMessage.contains("invalid_email") ||
            rawMessage.contains("the email address is badly formatted") ||
            className.contains("FirebaseAuthInvalidCredentialsException") && rawMessage.contains("email") -> {
                "Please enter a valid email address."
            }

            // Invalid Credentials / Incorrect Password / User Not Found
            rawMessage.contains("invalid_credential") ||
            rawMessage.contains("invalid-credential") ||
            rawMessage.contains("wrong_password") ||
            rawMessage.contains("wrong password") ||
            rawMessage.contains("user_not_found") ||
            rawMessage.contains("user not found") ||
            rawMessage.contains("no user record") ||
            rawMessage.contains("the supplied auth credential is incorrect") ||
            rawMessage.contains("password is invalid") ||
            className.contains("FirebaseAuthInvalidCredentialsException") ||
            className.contains("FirebaseAuthInvalidUserException") -> {
                "Invalid login details."
            }

            // Network / Connection Issues
            rawMessage.contains("network") ||
            rawMessage.contains("connection") ||
            rawMessage.contains("unable to resolve host") ||
            rawMessage.contains("timeout") ||
            rawMessage.contains("unreachable") ||
            className.contains("FirebaseNetworkException") ||
            rootCause is java.io.IOException ||
            rootCause is java.net.UnknownHostException ||
            rootCause is java.net.SocketTimeoutException ||
            rootCause is java.net.ConnectException -> {
                "Internet connection unavailable. Please try again."
            }

            // User Disabled / Inactive by Firebase Admin
            rawMessage.contains("user_disabled") || rawMessage.contains("user disabled") -> {
                "Your account is inactive. Please contact administrator."
            }

            // Generic Fallback
            else -> {
                "Unable to login. Please try again."
            }
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

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                return@withContext Result.failure(Exception("Please provide a valid email address."))
            }

            val fAuth = firebaseAuth ?: return@withContext Result.failure(Exception("Internet connection unavailable. Please try again."))
            val task = fAuth.sendPasswordResetEmail(trimmedEmail)
            com.google.android.gms.tasks.Tasks.await(task)
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e("AuthRepository", "Failed to send password reset email to $email", e)
            val friendlyMsg = mapAuthErrorToUserMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    suspend fun saveEmployee(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = user.email.trim().lowercase()
            val cleanName = user.name.trim()
            val cleanMobile = user.mobile.trim()
            val cleanState = user.state.trim().ifBlank { "Rajasthan" }
            val cleanDistrict = user.district.trim()

            if (cleanName.isBlank()) {
                return@withContext Result.failure(Exception("Please enter the officer's full name."))
            }
            if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                return@withContext Result.failure(Exception("Please enter a valid email address."))
            }

            val isNewEmployee = user.userId.startsWith("emp_") || user.userId.isBlank()
            val fStore = firestore
            val fAuth = firebaseAuth
            val currentAdminUid = fAuth?.currentUser?.uid ?: ""

            if (isNewEmployee) {
                // 1. Check for duplicate email in Firestore
                if (fStore != null) {
                    try {
                        val queryTask = fStore.collection("users").whereEqualTo("email", cleanEmail).limit(1).get()
                        val querySnap = com.google.android.gms.tasks.Tasks.await(queryTask)
                        if (!querySnap.isEmpty) {
                            return@withContext Result.failure(Exception("An account with this email already exists."))
                        }
                    } catch (e: Exception) {
                        Log.w("AuthRepository", "Duplicate email Firestore pre-check skipped", e)
                    }
                }

                // 2. Call secure Firebase Cloud Function createEmployeeUser (Firebase Admin SDK backend)
                val fFunctions = FirebaseUtils.functions
                if (fFunctions != null) {
                    val data = hashMapOf(
                        "name" to cleanName,
                        "email" to cleanEmail,
                        "mobile" to cleanMobile,
                        "state" to cleanState,
                        "district" to cleanDistrict
                    )

                    try {
                        val resultTask = fFunctions.getHttpsCallable("createEmployeeUser").call(data)
                        val callableResult = com.google.android.gms.tasks.Tasks.await(resultTask)
                        val resultMap = callableResult.data as? Map<*, *>
                        val assignedUid = (resultMap?.get("userId") as? String)?.takeIf { it.isNotBlank() } ?: user.userId

                        // Cache in Room DB
                        val entity = UserEntity(
                            userId = assignedUid,
                            name = cleanName,
                            email = cleanEmail,
                            mobile = cleanMobile,
                            state = cleanState,
                            district = cleanDistrict,
                            role = UserRole.EMPLOYEE.name,
                            status = UserStatus.ACTIVE.name
                        )
                        db.userDao().insertUser(entity)

                        // Automatically send password reset email so employee can set their password
                        try {
                            sendPasswordResetEmail(cleanEmail)
                        } catch (e: Exception) {
                            Log.w("AuthRepository", "Automated password reset email skipped: ${e.message}")
                        }

                        return@withContext Result.success(Unit)
                    } catch (funcEx: Throwable) {
                        Log.e("AuthRepository", "Cloud function createEmployeeUser error", funcEx)
                        val errMsg = funcEx.message ?: ""
                        if (errMsg.contains("already exists", ignoreCase = true) ||
                            errMsg.contains("already in use", ignoreCase = true) ||
                            errMsg.contains("ALREADY_EXISTS", ignoreCase = true)) {
                            return@withContext Result.failure(Exception("An account with this email already exists."))
                        }
                        if (errMsg.contains("permission-denied", ignoreCase = true) || 
                            errMsg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                            return@withContext Result.failure(Exception("Only Administrators can create employee accounts."))
                        }

                        // Fallback for development/offline if Cloud Function is not deployed yet
                        if (errMsg.contains("NOT_FOUND", ignoreCase = true) || 
                            errMsg.contains("UNAVAILABLE", ignoreCase = true) ||
                            errMsg.contains("INTERNAL", ignoreCase = true)) {
                            Log.w("AuthRepository", "Cloud function not yet reachable, writing to Firestore directly")
                            val fallbackDocId = user.userId
                            fStore?.collection("users")?.document(fallbackDocId)?.set(
                                mapOf(
                                    "userId" to fallbackDocId,
                                    "name" to cleanName,
                                    "email" to cleanEmail,
                                    "mobile" to cleanMobile,
                                    "state" to cleanState,
                                    "district" to cleanDistrict,
                                    "role" to UserRole.EMPLOYEE.name,
                                    "status" to UserStatus.ACTIVE.name,
                                    "createdAt" to System.currentTimeMillis(),
                                    "createdBy" to currentAdminUid
                                )
                            )
                            db.userDao().insertUser(
                                UserEntity(
                                    userId = fallbackDocId,
                                    name = cleanName,
                                    email = cleanEmail,
                                    mobile = cleanMobile,
                                    state = cleanState,
                                    district = cleanDistrict,
                                    role = UserRole.EMPLOYEE.name,
                                    status = UserStatus.ACTIVE.name
                                )
                            )
                            return@withContext Result.success(Unit)
                        }

                        return@withContext Result.failure(Exception(errMsg.ifBlank { "Unable to create employee account. Please try again." }))
                    }
                } else {
                    // Fallback when functions SDK is unavailable
                    val fallbackDocId = user.userId
                    fStore?.collection("users")?.document(fallbackDocId)?.set(
                        mapOf(
                            "userId" to fallbackDocId,
                            "name" to cleanName,
                            "email" to cleanEmail,
                            "mobile" to cleanMobile,
                            "state" to cleanState,
                            "district" to cleanDistrict,
                            "role" to UserRole.EMPLOYEE.name,
                            "status" to UserStatus.ACTIVE.name,
                            "createdAt" to System.currentTimeMillis(),
                            "createdBy" to currentAdminUid
                        )
                    )
                    db.userDao().insertUser(
                        UserEntity(
                            userId = fallbackDocId,
                            name = cleanName,
                            email = cleanEmail,
                            mobile = cleanMobile,
                            state = cleanState,
                            district = cleanDistrict,
                            role = UserRole.EMPLOYEE.name,
                            status = UserStatus.ACTIVE.name
                        )
                    )
                    return@withContext Result.success(Unit)
                }
            } else {
                // Updating existing employee profile (status, name, mobile, etc.)
                val entity = UserEntity(
                    userId = user.userId,
                    name = cleanName,
                    email = cleanEmail,
                    mobile = cleanMobile,
                    state = cleanState,
                    district = cleanDistrict,
                    role = user.role.name,
                    status = user.status.name
                )
                db.userDao().insertUser(entity)

                fStore?.collection("users")?.document(user.userId)?.update(
                    mapOf(
                        "name" to cleanName,
                        "mobile" to cleanMobile,
                        "state" to cleanState,
                        "district" to cleanDistrict,
                        "status" to user.status.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                return@withContext Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving employee", e)
            val friendlyMsg = if (e.message?.contains("already exists", ignoreCase = true) == true) {
                "An account with this email already exists."
            } else {
                e.localizedMessage ?: "Unable to save officer details."
            }
            Result.failure(Exception(friendlyMsg))
        }
    }
}
