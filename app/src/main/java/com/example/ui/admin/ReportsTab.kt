package com.example.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.ui.components.SearchTextField
import com.example.ui.components.StatusChip
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.ExcelHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTab(
    visits: List<Visit>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }

    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }

    var selectedVisitForDetails by remember { mutableStateOf<Visit?>(null) }
    val context = LocalContext.current

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

    val filteredVisits = remember(visits, searchQuery, selectedState, selectedDistrict, selectedBlock) {
        visits.filter { v ->
            val vState = v.state.ifBlank { "Rajasthan" }
            val matchState = selectedState == "All States" || vState == selectedState
            val matchDistrict = selectedDistrict == "All Districts" || v.district == selectedDistrict
            val matchBlock = selectedBlock == "All Blocks" || v.block == selectedBlock
            val matchQuery = searchQuery.isBlank() || (
                v.schoolName.contains(searchQuery, ignoreCase = true) ||
                v.district.contains(searchQuery, ignoreCase = true) ||
                v.block.contains(searchQuery, ignoreCase = true) ||
                v.employeeName.contains(searchQuery, ignoreCase = true)
            )
            matchState && matchDistrict && matchBlock && matchQuery
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
                        ExcelHelper.exportVisitsToCsv(context, filteredVisits)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
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

    // Detailed Visit Answers Dialog
    if (selectedVisitForDetails != null) {
        val v = selectedVisitForDetails!!
        val answers = remember(v) {
            try {
                Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter(VisitAnswers::class.java).fromJson(v.answersJson)
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { selectedVisitForDetails = null },
            title = { Text("Visit Report - ${v.schoolName}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Field Officer: ${v.employeeName}", fontWeight = FontWeight.Bold, color = Indigo600)
                    Text("Visit Date: ${v.visitDate}", fontSize = 12.sp, color = Slate500)

                    if (answers != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ReportAnswerItem("Met Principal?", answers.q9_metPrincipal)
                        ReportAnswerItem("App Awareness", answers.q10_missionGyanAwareness)
                        ReportAnswerItem("Student Count", answers.q11_studentCount)
                        ReportAnswerItem("School Response", answers.q12_schoolResponse)
                        ReportAnswerItem("WhatsApp Group Added", answers.q14_whatsappGroupAdded)
                        ReportAnswerItem("Poster Installed", answers.q15_posterInstalled)
                        ReportAnswerItem("Smart Class Status", answers.q21_smartClassStatus)
                        ReportAnswerItem("Key Observations", answers.q16_keyObservations)
                        ReportAnswerItem("Problems / Support Needed", answers.q17_problemsOrAssistance)
                        ReportAnswerItem("Follow-up Required", answers.q18_followupRequired)
                        ReportAnswerItem("Final Remarks", answers.q20_finalRemarks)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedVisitForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ReportAnswerItem(label: String, value: String) {
    if (value.isNotBlank()) {
        Column(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
            Text(value, fontSize = 13.sp, color = Slate700)
        }
    }
}
