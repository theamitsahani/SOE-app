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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.School
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.data.model.VisitStatus
import com.example.ui.components.SearchTextField
import com.example.ui.components.StatusChip
import com.example.ui.components.VisitDetailDialog
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.ExcelHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTab(
    visits: List<Visit>,
    schools: List<School> = emptyList(),
    initialStatusFilter: String = "All Statuses"
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(initialStatusFilter) }
    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }

    var statusExpanded by remember { mutableStateOf(false) }
    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }

    var selectedVisitForDetails by remember { mutableStateOf<Visit?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val statusList = listOf("All Statuses", "Completed", "Pending", "Follow-up Required")

    val stateList = remember(visits) {
        listOf("All States") + visits.map { if (it.state.isNotBlank()) it.state else "Rajasthan" }.distinct()
    }

    val districtList = remember(visits, selectedState) {
        val base = if (selectedState == "All States") visits else visits.filter { (it.state.ifBlank { "Rajasthan" }) == selectedState }
        listOf("All Districts") + base.map { it.district }.filter { it.isNotBlank() }.distinct()
    }

    val blockList = remember(visits, selectedState, selectedDistrict) {
        val base = visits.filter {
            (selectedState == "All States" || (it.state.ifBlank { "Rajasthan" }) == selectedState) &&
            (selectedDistrict == "All Districts" || it.district == selectedDistrict)
        }
        listOf("All Blocks") + base.map { it.block }.filter { it.isNotBlank() }.distinct()
    }

    val filteredVisits = remember(visits, searchQuery, selectedStatus, selectedState, selectedDistrict, selectedBlock) {
        visits.filter { v ->
            val vState = v.state.ifBlank { "Rajasthan" }
            val matchState = selectedState == "All States" || vState == selectedState
            val matchDistrict = selectedDistrict == "All Districts" || v.district == selectedDistrict
            val matchBlock = selectedBlock == "All Blocks" || v.block == selectedBlock
            
            val matchStatus = when (selectedStatus) {
                "Completed" -> v.status == VisitStatus.SUBMITTED || v.status == VisitStatus.REVIEWED
                "Pending" -> v.status == VisitStatus.ASSIGNED || v.status == VisitStatus.STARTED
                "Follow-up Required" -> v.answersJson.contains("\"q18_followupRequired\":\"हाँ\"")
                else -> true
            }

            val matchQuery = searchQuery.isBlank() || (
                v.schoolName.contains(searchQuery, ignoreCase = true) ||
                v.district.contains(searchQuery, ignoreCase = true) ||
                v.block.contains(searchQuery, ignoreCase = true) ||
                v.employeeName.contains(searchQuery, ignoreCase = true)
            )
            matchState && matchDistrict && matchBlock && matchStatus && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Visit Reports", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("${visits.size} submitted field reports", fontSize = 12.sp, color = Slate500)
                }

                Button(
                    onClick = {
                        showExportDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Status Filter Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "Filter Status: $selectedStatus",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Visit Status Filter", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusList.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st, fontSize = 13.sp, fontWeight = if (selectedStatus == st) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedStatus = st
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // State Filter
                    ExposedDropdownMenuBox(
                        expanded = stateExpanded,
                        onExpandedChange = { stateExpanded = !stateExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedState,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("State", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = stateExpanded,
                            onDismissRequest = { stateExpanded = false }
                        ) {
                            stateList.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, fontSize = 12.sp) },
                                    onClick = {
                                        selectedState = s
                                        selectedDistrict = "All Districts"
                                        selectedBlock = "All Blocks"
                                        stateExpanded = false
                                    }
                                )
                            }
                        }
                    }

                // District Filter
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDistrict,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("District", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        districtList.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d, fontSize = 12.sp) },
                                onClick = {
                                    selectedDistrict = d
                                    selectedBlock = "All Blocks"
                                    districtExpanded = false
                                }
                            )
                        }
                    }
                }

                // Block Filter
                ExposedDropdownMenuBox(
                    expanded = blockExpanded,
                    onExpandedChange = { blockExpanded = !blockExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedBlock,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Block", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = blockExpanded,
                        onDismissRequest = { blockExpanded = false }
                    ) {
                        blockList.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b, fontSize = 12.sp) },
                                onClick = {
                                    selectedBlock = b
                                    blockExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        }

        item {
            SearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search reports by school, district, block, or officer..."
            )
        }

        if (filteredVisits.isEmpty()) {
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
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No reports found matching criteria", fontSize = 14.sp, color = Slate500)
                    }
                }
            }
        } else {
            items(filteredVisits) { visit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedVisitForDetails = visit },
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
                            Text(visit.schoolName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900, modifier = Modifier.weight(1f))
                            StatusChip(statusName = visit.status.name)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("District: ${visit.district} • Block: ${visit.block}", fontSize = 12.sp, color = Slate500)

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Officer: ${visit.employeeName}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate700)
                            Text("Date: ${visit.visitDate}", fontSize = 12.sp, color = Slate500)
                        }
                    }
                }
            }
        }
    }

    // Comprehensive Single-Page Aligned Visit Detail Dialog
    selectedVisitForDetails?.let { visit ->
        val matchedSchool = remember(visit.schoolId, schools) {
            schools.find { it.schoolId == visit.schoolId }
        }

        VisitDetailDialog(
            visit = visit,
            school = matchedSchool,
            onDismiss = { selectedVisitForDetails = null }
        )
    }

    // Format Selection Dialog (PDF, Excel, CSV)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Indigo600.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Export Reports",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Select format for ${filteredVisits.size} visit records",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // PDF Option
                    ExportOptionCard(
                        title = "PDF Document (.pdf)",
                        subtitle = "Printable summary report with headers and table pages",
                        badgeColor = Color(0xFFDC2626),
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            showExportDialog = false
                            ExcelHelper.exportVisitsToPdf(context, filteredVisits)
                        }
                    )

                    // Excel Option
                    ExportOptionCard(
                        title = "Excel Spreadsheet (.xlsx)",
                        subtitle = "Complete dataset for Microsoft Excel & Google Sheets",
                        badgeColor = Color(0xFF16A34A),
                        icon = Icons.Default.TableChart,
                        onClick = {
                            showExportDialog = false
                            ExcelHelper.exportVisitsToExcel(context, filteredVisits)
                        }
                    )

                    // CSV Option
                    ExportOptionCard(
                        title = "CSV File (.csv)",
                        subtitle = "Comma-separated text file with UTF-8 BOM encoding",
                        badgeColor = Color(0xFF2563EB),
                        icon = Icons.Default.Description,
                        onClick = {
                            showExportDialog = false
                            ExcelHelper.exportVisitsToCsv(context, filteredVisits)
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Medium, color = Slate500)
                }
            }
        )
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    badgeColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text(subtitle, fontSize = 11.sp, color = Slate500)
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
