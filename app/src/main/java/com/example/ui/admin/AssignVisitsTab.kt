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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.ui.components.StatusChip
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate900
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignVisitsTab(
    schools: List<School>,
    employees: List<User>,
    assignedTasks: List<Task>,
    visits: List<Visit> = emptyList(),
    onAssignTask: (
        school: School,
        employee: User,
        visitDate: String,
        notes: String,
        onComplete: (Result<Task>) -> Unit
    ) -> Unit
) {
    var selectedSchool by remember { mutableStateOf<School?>(null) }
    var selectedEmployee1 by remember { mutableStateOf<User?>(null) }
    var selectedEmployee2 by remember { mutableStateOf<User?>(null) }
    var enableCoOfficer by remember { mutableStateOf(false) }
    var visitDate by remember { mutableStateOf("15-Aug-2026") }
    var notes by remember { mutableStateOf("") }

    var selectedState by remember { mutableStateOf("All States") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var selectedBlock by remember { mutableStateOf("All Blocks") }

    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }
    var schoolDropdownExpanded by remember { mutableStateOf(false) }
    var employee1DropdownExpanded by remember { mutableStateOf(false) }
    var employee2DropdownExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedVisitForDetails by remember { mutableStateOf<Visit?>(null) }

    // Exclude schools that are already assigned to a task or have a visit completed/submitted/reviewed
    val completedOrAssignedSchoolIds = remember(assignedTasks, visits, schools) {
        val fromTasks = assignedTasks.map { it.schoolId }
        val fromVisits = visits.map { it.schoolId }
        val fromSchoolDates = schools.filter { it.visitDate.isNotBlank() }.map { it.schoolId }
        (fromTasks + fromVisits + fromSchoolDates).toSet()
    }

    val availableSchools = remember(schools, completedOrAssignedSchoolIds) {
        schools.filter { school -> !completedOrAssignedSchoolIds.contains(school.schoolId) }
    }

    val stateList = remember(availableSchools) {
        listOf("All States") + availableSchools.map { if (it.state.isNotBlank()) it.state else "Rajasthan" }.distinct()
    }

    val districtList = remember(availableSchools, selectedState) {
        val base = if (selectedState == "All States") availableSchools else availableSchools.filter { (it.state.ifBlank { "Rajasthan" }) == selectedState }
        listOf("All Districts") + base.map { it.district }.filter { it.isNotBlank() }.distinct()
    }

    val blockList = remember(availableSchools, selectedState, selectedDistrict) {
        val base = availableSchools.filter {
            (selectedState == "All States" || (it.state.ifBlank { "Rajasthan" }) == selectedState) &&
            (selectedDistrict == "All Districts" || it.district == selectedDistrict)
        }
        listOf("All Blocks") + base.map { it.block }.filter { it.isNotBlank() }.distinct()
    }

    val filteredSchools = remember(availableSchools, selectedState, selectedDistrict, selectedBlock) {
        availableSchools.filter { school ->
            val sState = school.state.ifBlank { "Rajasthan" }
            (selectedState == "All States" || sState == selectedState) &&
            (selectedDistrict == "All Districts" || school.district == selectedDistrict) &&
            (selectedBlock == "All Blocks" || school.block == selectedBlock)
        }
    }

    // Only ACTIVE employees can be assigned tasks; inactive employees are excluded
    val activeEmployees = remember(employees) {
        employees.filter { it.status == UserStatus.ACTIVE }
    }

    // Determine target district for officer filtering
    val targetDistrict = remember(selectedSchool, selectedDistrict) {
        val d = selectedSchool?.district?.ifBlank { selectedDistrict } ?: selectedDistrict
        if (d == "All Districts") "" else d
    }

    val filteredEmployees = remember(activeEmployees, targetDistrict) {
        if (targetDistrict.isBlank()) {
            activeEmployees
        } else {
            val matched = activeEmployees.filter { it.district.equals(targetDistrict, ignoreCase = true) }
            if (matched.isNotEmpty()) matched else activeEmployees
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Slate900,
        unfocusedTextColor = Slate900,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Indigo600,
        unfocusedBorderColor = Slate300
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Assign New School Visit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
        }

        // Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (message != null) {
                        Text(message!!, color = Indigo600, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("Target Location Category (स्थान फ़िल्टर)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo600)

                    // Row 1: State & District Dropdowns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // State Dropdown
                        ExposedDropdownMenuBox(
                            expanded = stateExpanded,
                            onExpandedChange = { stateExpanded = !stateExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedState,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("State (राज्य)", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = stateExpanded,
                                onDismissRequest = { stateExpanded = false }
                            ) {
                                stateList.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, fontSize = 13.sp) },
                                        onClick = {
                                            selectedState = s
                                            selectedDistrict = "All Districts"
                                            selectedBlock = "All Blocks"
                                            selectedSchool = null
                                            stateExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // District Dropdown
                        ExposedDropdownMenuBox(
                            expanded = districtExpanded,
                            onExpandedChange = { districtExpanded = !districtExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedDistrict,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("District (जिला)", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = districtExpanded,
                                onDismissRequest = { districtExpanded = false }
                            ) {
                                districtList.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDistrict = d
                                            selectedBlock = "All Blocks"
                                            selectedSchool = null
                                            districtExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Block Dropdown (Full Width for complete readability)
                    ExposedDropdownMenuBox(
                        expanded = blockExpanded,
                        onExpandedChange = { blockExpanded = !blockExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBlock,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Block (ब्लॉक)", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = blockExpanded,
                            onDismissRequest = { blockExpanded = false }
                        ) {
                            blockList.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b, fontSize = 13.sp) },
                                    onClick = {
                                        selectedBlock = b
                                        selectedSchool = null
                                        blockExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Select School Dropdown
                    ExposedDropdownMenuBox(
                        expanded = schoolDropdownExpanded,
                        onExpandedChange = { schoolDropdownExpanded = !schoolDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSchool?.schoolName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target School (${filteredSchools.size} unassigned)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = schoolDropdownExpanded,
                            onDismissRequest = { schoolDropdownExpanded = false }
                        ) {
                            if (filteredSchools.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No unassigned schools available (All assigned/completed)", color = Slate500, fontSize = 12.sp) },
                                    onClick = { schoolDropdownExpanded = false }
                                )
                            } else {
                                filteredSchools.forEach { school ->
                                    DropdownMenuItem(
                                        text = { Text("${school.schoolName} (${school.block}, ${school.district})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedSchool = school
                                            schoolDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // District Filter Info Badge for Officers
                    if (targetDistrict.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Indigo600.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Indigo600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Active Officers in $targetDistrict: ${filteredEmployees.size} available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Indigo600
                                )
                            }
                        }
                    }

                    // Primary Officer Selection
                    ExposedDropdownMenuBox(
                        expanded = employee1DropdownExpanded,
                        onExpandedChange = { employee1DropdownExpanded = !employee1DropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedEmployee1?.let { "${it.name} (${it.district.ifBlank { "Unassigned" }})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Primary Field Officer (सक्रिय कर्मचारी 1) *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employee1DropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = employee1DropdownExpanded,
                            onDismissRequest = { employee1DropdownExpanded = false }
                        ) {
                            if (filteredEmployees.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No active officers available", color = Slate500, fontSize = 12.sp) },
                                    onClick = { employee1DropdownExpanded = false }
                                )
                            } else {
                                filteredEmployees.forEach { emp ->
                                    DropdownMenuItem(
                                        text = { Text("${emp.name} • ${emp.district} (${emp.email})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedEmployee1 = emp
                                            employee1DropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Optional Secondary Co-Officer Toggle & Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assign 2nd Co-Officer to Same School? (2 कर्मचारी जोड़ें)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                        Switch(
                            checked = enableCoOfficer,
                            onCheckedChange = {
                                enableCoOfficer = it
                                if (!it) selectedEmployee2 = null
                            }
                        )
                    }

                    if (enableCoOfficer) {
                        ExposedDropdownMenuBox(
                            expanded = employee2DropdownExpanded,
                            onExpandedChange = { employee2DropdownExpanded = !employee2DropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedEmployee2?.let { "${it.name} (${it.district.ifBlank { "Unassigned" }})" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Secondary Co-Officer (कर्मचारी 2)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employee2DropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = employee2DropdownExpanded,
                                onDismissRequest = { employee2DropdownExpanded = false }
                            ) {
                                filteredEmployees.filter { it.userId != selectedEmployee1?.userId }.forEach { emp ->
                                    DropdownMenuItem(
                                        text = { Text("${emp.name} • ${emp.district} (${emp.email})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedEmployee2 = emp
                                            employee2DropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = visitDate,
                        onValueChange = { visitDate = it },
                        label = { Text("Visit Scheduled Date") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Instructions / Guidelines") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (selectedSchool != null && selectedEmployee1 != null) {
                                isSubmitting = true
                                var assignedCount = 0
                                var hasError = false
                                var errorMsg = ""

                                fun checkDone() {
                                    isSubmitting = false
                                    if (!hasError) {
                                        val officersText = if (enableCoOfficer && selectedEmployee2 != null)
                                            "${selectedEmployee1?.name} & ${selectedEmployee2?.name}"
                                        else "${selectedEmployee1?.name}"
                                        message = "Task successfully assigned to $officersText!"
                                        selectedSchool = null
                                        selectedEmployee1 = null
                                        selectedEmployee2 = null
                                        notes = ""
                                    } else {
                                        message = "Error assigning task: $errorMsg"
                                    }
                                }

                                onAssignTask(selectedSchool!!, selectedEmployee1!!, visitDate, notes) { res1 ->
                                    if (res1.isFailure) {
                                        hasError = true
                                        errorMsg = res1.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                    }
                                    assignedCount++
                                    if (!enableCoOfficer || selectedEmployee2 == null) {
                                        checkDone()
                                    } else {
                                        onAssignTask(selectedSchool!!, selectedEmployee2!!, visitDate, notes) { res2 ->
                                            if (res2.isFailure) {
                                                hasError = true
                                                errorMsg = res2.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                            }
                                            assignedCount++
                                            checkDone()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting && selectedSchool != null && selectedEmployee1 != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (enableCoOfficer && selectedEmployee2 != null) "Assign Visit Task to Both Officers" else "Confirm & Assign Visit Task",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text("Recently Assigned Tasks", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
        }

        if (assignedTasks.isEmpty()) {
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
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = Slate500, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No tasks currently assigned", fontSize = 14.sp, color = Slate500)
                    }
                }
            }
        } else {
            items(assignedTasks) { task ->
                val matchedVisit = remember(task, visits) {
                    visits.find { it.schoolId == task.schoolId || it.schoolName == task.schoolName }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (matchedVisit != null) {
                                selectedVisitForDetails = matchedVisit
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(task.schoolName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900, modifier = Modifier.weight(1f))
                            StatusChip(statusName = task.status.name)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Assigned To: ${task.employeeName} • Date: ${task.visitDate}", fontSize = 12.sp, color = Slate700)
                        if (matchedVisit != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("👉 Click to read submitted visit report details", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

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
            title = { Text("Submitted Report - ${v.schoolName}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                        ReportAnswerItem("App Knowledge", answers.q10_missionGyanAwareness)
                        ReportAnswerItem("Student Attendance", answers.q11_studentCount)
                        ReportAnswerItem("School Response", answers.q12_schoolResponse)
                        ReportAnswerItem("WhatsApp Group", answers.q14_whatsappGroupAdded)
                        ReportAnswerItem("Poster Installed", answers.q15_posterInstalled)
                        ReportAnswerItem("Smart Class Status", answers.q21_smartClassStatus)
                        ReportAnswerItem("Key Observations", answers.q16_keyObservations)
                        ReportAnswerItem("Help Needed", answers.q17_problemsOrAssistance)
                        ReportAnswerItem("Follow-up Needed", answers.q18_followupRequired)
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
