package com.example.ui.admin

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Visit
import com.example.data.model.VisitStatus
import com.example.ui.components.StatusChip
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Teal600

@Composable
fun AdminDashboardTab(
    visits: List<Visit>,
    totalSchoolsCount: Int,
    totalEmployeesCount: Int,
    onNavigateTab: (AdminTab) -> Unit,
    onNavigateTabWithFilter: (AdminTab, String) -> Unit = { tab, _ -> onNavigateTab(tab) },
    onVisitClick: (Visit) -> Unit
) {
    val completedCount = visits.count { it.status == VisitStatus.SUBMITTED || it.status == VisitStatus.REVIEWED }
    val pendingCount = visits.count { it.status == VisitStatus.ASSIGNED || it.status == VisitStatus.STARTED }
    val followUpCount = visits.count { it.answersJson.contains("\"q18_followupRequired\":\"हाँ\"") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Visit Summary & Metrics (टैप करके फ़िल्टर देखें)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Completed",
                        value = completedCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Emerald600,
                        bgColor = Emerald100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Completed") },
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Pending / In Progress",
                        value = pendingCount.toString(),
                        icon = Icons.Default.HourglassTop,
                        color = Amber600,
                        bgColor = Amber100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Pending") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Follow-up Required",
                        value = followUpCount.toString(),
                        icon = Icons.Default.Warning,
                        color = Red600,
                        bgColor = Red100,
                        onClick = { onNavigateTabWithFilter(AdminTab.VISIT_REPORTS, "Follow-up Required") },
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Schools Enrolled",
                        value = totalSchoolsCount.toString(),
                        icon = Icons.Default.School,
                        color = Indigo600,
                        bgColor = Color(0xFFE0E7FF),
                        onClick = { onNavigateTabWithFilter(AdminTab.SCHOOLS, "") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickActionButton(
                            title = "Assign Visit",
                            icon = Icons.Default.AssignmentTurnedIn,
                            onClick = { onNavigateTab(AdminTab.ASSIGN_VISITS) }
                        )
                        QuickActionButton(
                            title = "Import Excel",
                            icon = Icons.Default.School,
                            onClick = { onNavigateTab(AdminTab.SCHOOLS) }
                        )
                        QuickActionButton(
                            title = "Photo Gallery",
                            icon = Icons.Default.PhotoLibrary,
                            onClick = { onNavigateTab(AdminTab.PHOTO_GALLERY) }
                        )
                        QuickActionButton(
                            title = "Reports",
                            icon = Icons.Default.Download,
                            onClick = { onNavigateTab(AdminTab.VISIT_REPORTS) }
                        )
                    }
                }
            }
        }

        // Recent Visits Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Visit Reports",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Indigo600,
                    modifier = Modifier.clickable { onNavigateTab(AdminTab.VISIT_REPORTS) }
                )
            }
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No visit reports submitted yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                    }
                }
            }
        } else {
            items(visits.take(5)) { visit ->
                VisitCardItem(visit = visit, onClick = { onVisitClick(visit) })
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = title, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEF2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Indigo600, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Slate700)
    }
}

@Composable
fun VisitCardItem(
    visit: Visit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = visit.schoolName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(statusName = visit.status.name)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${visit.district} • ${visit.block}",
                fontSize = 12.sp,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Officer: ${visit.employeeName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700
                )
                Text(
                    text = visit.visitDate,
                    fontSize = 12.sp,
                    color = Slate500
                )
            }
        }
    }
}
