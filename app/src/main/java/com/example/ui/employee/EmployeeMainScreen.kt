package com.example.ui.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.ui.components.StatusChip
import com.example.ui.components.SyncStatusBanner
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700

enum class EmployeeNavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODAY_TASKS("Today's Tasks", Icons.Default.Home),
    UPCOMING("Upcoming Tasks", Icons.Default.Assignment),
    COMPLETED("Completed Visits", Icons.Default.CheckCircle),
    PROFILE("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeMainScreen(
    employeeUser: User,
    tasks: List<Task>,
    completedVisits: List<Visit>,
    isOnline: Boolean,
    pendingSyncCount: Int,
    onSyncClick: () -> Unit,
    onStartVisit: (Task) -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = EmployeeNavTab.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Indigo600),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Mission Gyan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            Text("Field Officer Portal", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Slate700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Indigo600,
                            selectedTextColor = Indigo600,
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500,
                            indicatorColor = Color(0xFFEEF2FF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate100)
        ) {
            SyncStatusBanner(
                isOnline = isOnline,
                pendingCount = pendingSyncCount,
                onSyncClick = onSyncClick
            )

            when (tabs[selectedTab]) {
                EmployeeNavTab.TODAY_TASKS -> {
                    TasksListSection(
                        title = "Today's Assigned Tasks",
                        tasks = tasks,
                        onStartVisit = onStartVisit
                    )
                }
                EmployeeNavTab.UPCOMING -> {
                    TasksListSection(
                        title = "Upcoming Field Tasks",
                        tasks = tasks.filter { it.status == VisitStatus.ASSIGNED },
                        onStartVisit = onStartVisit
                    )
                }
                EmployeeNavTab.COMPLETED -> {
                    CompletedVisitsSection(visits = completedVisits)
                }
                EmployeeNavTab.PROFILE -> {
                    EmployeeProfileSection(user = employeeUser, onLogout = onLogoutClick)
                }
            }
        }
    }
}

@Composable
fun TasksListSection(
    title: String,
    tasks: List<Task>,
    onStartVisit: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No tasks assigned for today", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Text("Check back later or contact your admin", fontSize = 12.sp, color = Slate500)
                    }
                }
            }
        } else {
            items(tasks) { task ->
                TaskCardItem(task = task, onStartVisit = { onStartVisit(task) })
            }
        }
    }
}

@Composable
fun TaskCardItem(
    task: Task,
    onStartVisit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(statusName = task.status.name)
                Text("Date: ${task.visitDate}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate500)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.schoolName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Block: ${task.block} • District: ${task.district}",
                fontSize = 13.sp,
                color = Slate500
            )

            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${task.notes}",
                        fontSize = 12.sp,
                        color = Slate700,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isSubmitted = task.status == VisitStatus.SUBMITTED || task.status == VisitStatus.REVIEWED

            Button(
                onClick = onStartVisit,
                enabled = !isSubmitted,
                shape = RoundedCornerShape(12.dp),
                colors = if (isSubmitted) {
                    ButtonDefaults.buttonColors(containerColor = Emerald600, disabledContainerColor = Emerald600, disabledContentColor = Color.White)
                } else {
                    ButtonDefaults.buttonColors(containerColor = Indigo600)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isSubmitted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSubmitted) "Task Submitted & Completed" else "Start School Visit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CompletedVisitsSection(visits: List<Visit>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Your Completed Visits", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Navy900)
        }

        if (visits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No completed visit reports yet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    }
                }
            }
        } else {
            items(visits) { visit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(visit.schoolName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900, modifier = Modifier.weight(1f))
                            StatusChip(statusName = visit.status.name)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${visit.district} • ${visit.block}", fontSize = 12.sp, color = Slate500)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Submitted on: ${visit.visitDate}", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeProfileSection(user: User, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Indigo600),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(user.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text(user.email, fontSize = 13.sp, color = Slate500)
                if (user.mobile.isNotBlank()) {
                    Text("Mobile: ${user.mobile}", fontSize = 13.sp, color = Slate500)
                }

                Spacer(modifier = Modifier.height(12.dp))

                StatusChip(statusName = user.status.name)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
