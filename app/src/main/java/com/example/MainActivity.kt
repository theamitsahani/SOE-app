package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.Visit
import com.example.data.repository.AuthRepository
import com.example.data.repository.SchoolRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.VisitRepository
import com.example.ui.admin.AdminDashboardTab
import com.example.ui.admin.AdminMainScreen
import com.example.ui.admin.AdminTab
import com.example.ui.admin.AssignVisitsTab
import com.example.ui.admin.EmployeeManagementTab
import com.example.ui.admin.PhotoGalleryTab
import com.example.ui.admin.ReportsTab
import com.example.ui.admin.SchoolManagementTab
import com.example.ui.admin.SettingsTab
import com.example.ui.auth.LoginScreen
import com.example.ui.employee.EmployeeMainScreen
import com.example.ui.employee.VisitFormScreen
import com.example.ui.theme.SOETheme
import com.example.util.FirebaseUtils
import com.example.util.SyncManager
import kotlinx.coroutines.launch

sealed class ScreenState {
    data object Login : ScreenState()
    data class Admin(val adminUser: User) : ScreenState()
    data class Employee(val employeeUser: User) : ScreenState()
    data class VisitForm(val employeeUser: User, val task: Task?, val school: School?) : ScreenState()
}

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var schoolRepository: SchoolRepository
    private lateinit var visitRepository: VisitRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseUtils.initialize(applicationContext)

        authRepository = AuthRepository(applicationContext)
        schoolRepository = SchoolRepository(applicationContext)
        visitRepository = VisitRepository(applicationContext)
        taskRepository = TaskRepository(applicationContext)
        syncManager = SyncManager(applicationContext)

        setContent {
            SOETheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Login) }
                    var isInitializing by remember { mutableStateOf(true) }

                    val currentUser by authRepository.currentUser.collectAsState()
                    val schools by schoolRepository.getAllSchools().collectAsState(initial = emptyList())
                    val visits by visitRepository.getAllVisits().collectAsState(initial = emptyList())
                    val tasks by taskRepository.getAllTasks().collectAsState(initial = emptyList())
                    val employees by authRepository.getAllEmployees().collectAsState(initial = emptyList())

                    val isOnline by syncManager.isOnline.collectAsState()
                    val pendingSyncCount by syncManager.pendingSyncCount.collectAsState()

                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        authRepository.initializeDefaultAccounts()
                        schoolRepository.seedDefaultSchools()
                        visitRepository.seedDefaultVisits()
                        taskRepository.seedDefaultTasks()
                        syncManager.checkPendingCount()
                        isInitializing = false
                    }

                    if (isInitializing) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Crossfade(targetState = currentScreen, label = "ScreenTransition") { state ->
                            when (state) {
                                is ScreenState.Login -> {
                                    LoginScreen(
                                        onLoginClick = { email, pass, onResult ->
                                            scope.launch {
                                                val result = authRepository.login(email, pass)
                                                if (result.isSuccess) {
                                                    val user = result.getOrNull()!!
                                                    if (user.role == UserRole.ADMIN) {
                                                        currentScreen = ScreenState.Admin(user)
                                                    } else {
                                                        currentScreen = ScreenState.Employee(user)
                                                    }
                                                    onResult(Result.success(Unit))
                                                } else {
                                                    onResult(Result.failure(result.exceptionOrNull()!!))
                                                }
                                            }
                                        }
                                    )
                                }

                                is ScreenState.Admin -> {
                                    var selectedAdminTab by remember { mutableIntStateOf(0) }
                                    var reportsStatusFilter by remember { mutableStateOf("All Statuses") }

                                    AdminMainScreen(
                                        adminUser = state.adminUser,
                                        selectedTab = selectedAdminTab,
                                        onTabSelected = { selectedAdminTab = it },
                                        onLogoutClick = {
                                            authRepository.logout()
                                            currentScreen = ScreenState.Login
                                        }
                                    ) { currentTab ->
                                        when (currentTab) {
                                            AdminTab.DASHBOARD -> {
                                                AdminDashboardTab(
                                                    visits = visits,
                                                    totalSchoolsCount = schools.size,
                                                    totalEmployeesCount = employees.size,
                                                    onNavigateTab = { targetTab ->
                                                        selectedAdminTab = targetTab.ordinal
                                                    },
                                                    onNavigateTabWithFilter = { targetTab, filter ->
                                                        if (filter.isNotBlank()) {
                                                            reportsStatusFilter = filter
                                                        }
                                                        selectedAdminTab = targetTab.ordinal
                                                    },
                                                    onVisitClick = {
                                                        selectedAdminTab = AdminTab.VISIT_REPORTS.ordinal
                                                    }
                                                )
                                            }
                                            AdminTab.EMPLOYEES -> {
                                                EmployeeManagementTab(
                                                    employees = employees,
                                                    onSaveEmployee = { emp, callback ->
                                                        scope.launch {
                                                            val res = authRepository.saveEmployee(emp)
                                                            callback(res)
                                                        }
                                                    }
                                                )
                                            }
                                            AdminTab.SCHOOLS -> {
                                                SchoolManagementTab(
                                                    schools = schools,
                                                    onImportSchools = { newSchools, completedVisits, callback ->
                                                        scope.launch {
                                                            val res = schoolRepository.importSchools(newSchools)
                                                             for (v in completedVisits) {
                                                                 visitRepository.submitVisit(v)
                                                             }
                                                            callback(res)
                                                        }
                                                    },
                                                    onUpdateSchool = { sch ->
                                                        scope.launch { schoolRepository.updateSchoolRecord(sch) }
                                                    }
                                                )
                                            }
                                            AdminTab.ASSIGN_VISITS -> {
                                                AssignVisitsTab(
                                                    schools = schools,
                                                    employees = employees,
                                                    assignedTasks = tasks,
                                                    visits = visits,
                                                    onAssignTask = { sch, emp, date, notes, callback ->
                                                        scope.launch {
                                                            val res = taskRepository.assignTask(
                                                                schoolId = sch.schoolId,
                                                                schoolName = sch.schoolName,
                                                                district = sch.district,
                                                                block = sch.block,
                                                                employeeId = emp.userId,
                                                                employeeName = emp.name,
                                                                visitDate = date,
                                                                notes = notes
                                                            )
                                                            callback(res)
                                                        }
                                                    }
                                                )
                                            }
                                            AdminTab.VISIT_REPORTS -> {
                                                ReportsTab(visits = visits, initialStatusFilter = reportsStatusFilter)
                                            }
                                            AdminTab.PHOTO_GALLERY -> {
                                                PhotoGalleryTab(visits = visits)
                                            }
                                            AdminTab.EXPORT -> {
                                                ReportsTab(visits = visits)
                                            }
                                            AdminTab.SETTINGS -> {
                                                SettingsTab(
                                                    adminUser = state.adminUser,
                                                    onChangePassword = { newPass, callback ->
                                                        scope.launch {
                                                            val res = authRepository.updatePassword(newPass)
                                                            callback(res)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                is ScreenState.Employee -> {
                                    EmployeeMainScreen(
                                        employeeUser = state.employeeUser,
                                        tasks = tasks.filter { it.employeeId == state.employeeUser.userId || it.employeeId == "emp_001" },
                                        completedVisits = visits.filter { it.employeeId == state.employeeUser.userId },
                                        isOnline = isOnline,
                                        pendingSyncCount = pendingSyncCount,
                                        onSyncClick = {
                                            scope.launch { syncManager.syncPendingData() }
                                        },
                                        onStartVisit = { task ->
                                            val matchedSchool = schools.find { it.schoolId == task.schoolId }
                                            currentScreen = ScreenState.VisitForm(state.employeeUser, task, matchedSchool)
                                        },
                                        onLogoutClick = {
                                            authRepository.logout()
                                            currentScreen = ScreenState.Login
                                        }
                                    )
                                }

                                is ScreenState.VisitForm -> {
                                    VisitFormScreen(
                                        employeeUser = state.employeeUser,
                                        task = state.task,
                                        initialSchool = state.school,
                                        isOnline = isOnline,
                                        pendingSyncCount = pendingSyncCount,
                                        onBackClick = {
                                            currentScreen = ScreenState.Employee(state.employeeUser)
                                        },
                                        onSubmitVisit = { visit, callback ->
                                            scope.launch {
                                                val res = visitRepository.submitVisit(visit)
                                                if (res.isSuccess) {
                                                    val matchedTask = tasks.find { it.schoolId == visit.schoolId || it.visitId == visit.visitId }
                                                    if (matchedTask != null) {
                                                        taskRepository.updateTaskStatus(matchedTask.taskId, com.example.data.model.VisitStatus.SUBMITTED)
                                                    } else if (state.task != null) {
                                                        taskRepository.updateTaskStatus(state.task.taskId, com.example.data.model.VisitStatus.SUBMITTED)
                                                    }
                                                }
                                                callback(res)
                                            }
                                        },
                                        onUpdateSchoolInfo = { updatedSchool ->
                                            scope.launch {
                                                schoolRepository.updateSchoolRecord(updatedSchool)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
