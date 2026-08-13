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
    var selectedEmployee by remember { mutableStateOf<User?>(null) }
    var visitDate by remember { mutableStateOf("15-Aug-2026") }
    var notes by remember { mutableStateOf("") }

    var schoolDropdownExpanded by remember { mutableStateOf(false) }
    var employeeDropdownExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedVisitForDetails by remember { mutableStateOf<Visit?>(null) }

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

                    // Select School Dropdown
                    ExposedDropdownMenuBox(
                        expanded = schoolDropdownExpanded,
                        onExpandedChange = { schoolDropdownExpanded = !schoolDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSchool?.schoolName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target School") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = schoolDropdownExpanded,
                            onDismissRequest = { schoolDropdownExpanded = false }
                        ) {
                            schools.forEach { school ->
                                DropdownMenuItem(
                                    text = { Text(school.schoolName, fontSize = 13.sp) },
                                    onClick = {
                                        selectedSchool = school
                                        schoolDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Select Officer Dropdown
                    ExposedDropdownMenuBox(
                        expanded = employeeDropdownExpanded,
                        onExpandedChange = { employeeDropdownExpanded = !employeeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedEmployee?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign to Field Officer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = employeeDropdownExpanded,
                            onDismissRequest = { employeeDropdownExpanded = false }
                        ) {
                            employees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text("${emp.name} (${emp.email})", fontSize = 13.sp) },
                                    onClick = {
                                        selectedEmployee = emp
                                        employeeDropdownExpanded = false
                                    }
                                )
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
                            if (selectedSchool != null && selectedEmployee != null) {
                                isSubmitting = true
                                onAssignTask(
                                    selectedSchool!!,
                                    selectedEmployee!!,
                                    visitDate,
                                    notes
                                ) { res ->
                                    isSubmitting = false
                                    if (res.isSuccess) {
                                        message = "Task successfully assigned to ${selectedEmployee?.name}"
                                        selectedSchool = null
                                        selectedEmployee = null
                                        notes = ""
                                    } else {
                                        message = "Error: " + res.exceptionOrNull()?.localizedMessage
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting && selectedSchool != null && selectedEmployee != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm & Assign Visit Task", fontWeight = FontWeight.Bold)
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
