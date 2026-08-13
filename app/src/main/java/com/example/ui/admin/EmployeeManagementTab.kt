package com.example.ui.admin

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.ui.components.SearchTextField
import com.example.ui.components.StatusChip
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import java.util.UUID

@Composable
fun EmployeeManagementTab(
    employees: List<User>,
    onSaveEmployee: (User, (Result<Unit>) -> Unit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }

    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isBlank()) employees
        else employees.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true) ||
                    it.mobile.contains(searchQuery, ignoreCase = true)
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
                    Text("SOE Field Officers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("${employees.size} active & registered officers", fontSize = 12.sp, color = Slate500)
                }

                Button(
                    onClick = { showAddEmployeeDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Officer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            SearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search officer by name, email, or mobile..."
            )
        }

        if (filteredEmployees.isEmpty()) {
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
                        Icon(Icons.Default.Group, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No officers found matching search", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate500)
                    }
                }
            }
        } else {
            items(filteredEmployees) { emp ->
                EmployeeCardItem(
                    employee = emp,
                    onToggleStatus = { newStatus ->
                        onSaveEmployee(emp.copy(status = newStatus)) {}
                    }
                )
            }
        }
    }

    // Add Employee Dialog
    if (showAddEmployeeDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Add New Field Officer", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        val newEmp = User(
                            userId = "emp_" + UUID.randomUUID().toString().take(8),
                            name = name,
                            email = email,
                            mobile = mobile,
                            role = UserRole.EMPLOYEE,
                            status = UserStatus.ACTIVE
                        )
                        onSaveEmployee(newEmp) {
                            isSaving = false
                            showAddEmployeeDialog = false
                        }
                    },
                    enabled = !isSaving && name.isNotBlank() && email.isNotBlank()
                ) {
                    Text("Create Officer Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployeeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmployeeCardItem(
    employee: User,
    onToggleStatus: (UserStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(employee.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(statusName = employee.status.name)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(employee.email, fontSize = 12.sp, color = Slate500)
                if (employee.mobile.isNotBlank()) {
                    Text("Mobile: ${employee.mobile}", fontSize = 12.sp, color = Slate700)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (employee.status == UserStatus.ACTIVE) "Active" else "Inactive",
                    fontSize = 11.sp,
                    color = Slate500
                )
                Switch(
                    checked = employee.status == UserStatus.ACTIVE,
                    onCheckedChange = { checked ->
                        onToggleStatus(if (checked) UserStatus.ACTIVE else UserStatus.INACTIVE)
                    }
                )
            }
        }
    }
}
