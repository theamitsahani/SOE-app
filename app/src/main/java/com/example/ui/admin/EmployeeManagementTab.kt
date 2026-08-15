package com.example.ui.admin

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.ui.components.SearchTextField
import com.example.ui.components.StatusChip
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import java.util.UUID

@Composable
fun EmployeeManagementTab(
    employees: List<User>,
    onSaveEmployee: (User, (Result<Unit>) -> Unit) -> Unit,
    onResetPassword: ((String, (Result<Unit>) -> Unit) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<User?>(null) }
    var selectedEmployeeForDetails by remember { mutableStateOf<User?>(null) }
    var showResetPasswordConfirmFor by remember { mutableStateOf<User?>(null) }
    var isResettingPassword by remember { mutableStateOf(false) }
    var resetPasswordStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isBlank()) employees
        else employees.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true) ||
            it.mobile.contains(searchQuery, ignoreCase = true) ||
            it.district.contains(searchQuery, ignoreCase = true) ||
            it.state.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SOE Field Officers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("${employees.count { it.status == UserStatus.ACTIVE }} Active • ${employees.size} Total Officers", fontSize = 11.sp, color = Slate500)
                }

                Button(
                    onClick = { showAddEmployeeDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Officer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            SearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search officer by name, district, state..."
            )
        }

        if (filteredEmployees.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Slate500, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No officers found matching search", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate500)
                    }
                }
            }
        } else {
            items(filteredEmployees) { emp ->
                CompactEmployeeCardItem(
                    employee = emp,
                    onClick = { selectedEmployeeForDetails = emp },
                    onEditClick = { employeeToEdit = emp },
                    onToggleStatus = { newStatus ->
                        onSaveEmployee(emp.copy(status = newStatus)) {}
                    }
                )
            }
        }
    }

    // View Details Dialog on Tap
    if (selectedEmployeeForDetails != null) {
        val emp = selectedEmployeeForDetails!!
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { selectedEmployeeForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Navy900)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (emp.status == UserStatus.ACTIVE) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (emp.status == UserStatus.ACTIVE) "ACTIVE (सक्रिय)" else "INACTIVE (निष्क्रिय)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (emp.status == UserStatus.ACTIVE) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Location
                    DetailItem(label = "State & District (राज्य व जिला)", value = "${emp.state.ifBlank { "Rajasthan" }} • ${emp.district.ifBlank { "All Districts" }}")

                    // Email
                    DetailItem(label = "Email Address (ईमेल)", value = emp.email)

                    // Mobile Number with Direct Call Action
                    Column {
                        Text("Mobile Number (मोबाइल नंबर)", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = emp.mobile.ifBlank { "Not provided" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            if (emp.mobile.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${emp.mobile}"))
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = Indigo600)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 11.sp, color = Indigo600)
                                }
                            }
                        }
                    }

                    // Toggle Status Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate100)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Status (सक्रिय/निष्क्रिय)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate700)
                        Switch(
                            checked = emp.status == UserStatus.ACTIVE,
                            onCheckedChange = { checked ->
                                val newStatus = if (checked) UserStatus.ACTIVE else UserStatus.INACTIVE
                                val updated = emp.copy(status = newStatus)
                                selectedEmployeeForDetails = updated
                                onSaveEmployee(updated) {}
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Indigo600
                            )
                        )
                    }

                    // Reset Password Button for Admin
                    OutlinedButton(
                        onClick = {
                            val targetEmp = emp
                            selectedEmployeeForDetails = null
                            showResetPasswordConfirmFor = targetEmp
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Indigo600
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset Password (पासवर्ड रीसेट लिंक भेजें)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Indigo600
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toEdit = emp
                        selectedEmployeeForDetails = null
                        employeeToEdit = toEdit
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Details")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEmployeeForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Reset Password Confirmation Dialog
    if (showResetPasswordConfirmFor != null) {
        val emp = showResetPasswordConfirmFor!!
        AlertDialog(
            onDismissRequest = {
                if (!isResettingPassword) {
                    showResetPasswordConfirmFor = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Send a secure password reset link to ${emp.name}'s registered email address?",
                        fontSize = 13.sp,
                        color = Navy900
                    )
                    Text(
                        text = emp.email,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600
                    )
                    Text(
                        text = "The reset link is generated securely by Firebase Authentication. No passwords are saved or stored in the database.",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isResettingPassword = true
                        onResetPassword?.invoke(emp.email) { result ->
                            isResettingPassword = false
                            showResetPasswordConfirmFor = null
                            if (result.isSuccess) {
                                resetPasswordStatusMessage = Pair(true, "Password reset link sent successfully to ${emp.email}")
                            } else {
                                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to send reset link."
                                resetPasswordStatusMessage = Pair(false, err)
                            }
                        } ?: run {
                            isResettingPassword = false
                            showResetPasswordConfirmFor = null
                        }
                    },
                    enabled = !isResettingPassword && emp.email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isResettingPassword) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetPasswordConfirmFor = null },
                    enabled = !isResettingPassword
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Password Reset Result Feedback Dialog
    if (resetPasswordStatusMessage != null) {
        val (isSuccess, msg) = resetPasswordStatusMessage!!
        AlertDialog(
            onDismissRequest = { resetPasswordStatusMessage = null },
            title = {
                Text(
                    text = if (isSuccess) "Email Sent" else "Notice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Navy900
                )
            },
            text = {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = if (isSuccess) Navy900 else Red600
                )
            },
            confirmButton = {
                Button(
                    onClick = { resetPasswordStatusMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Add Employee Dialog
    if (showAddEmployeeDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("") }
        var state by remember { mutableStateOf("Rajasthan") }
        var district by remember { mutableStateOf("Jaipur") }
        var isSaving by remember { mutableStateOf(false) }
        var addErrorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddEmployeeDialog = false },
            title = { Text("Add New Field Officer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (addErrorMessage != null) {
                        Surface(
                            color = Red600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = addErrorMessage!!,
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            addErrorMessage = null
                        },
                        label = { Text("Full Name (नाम) *", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            addErrorMessage = null
                        },
                        label = { Text("Email Address (ईमेल) *", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number (मोबाइल)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State (राज्य)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District (जिला)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        addErrorMessage = null
                        val newEmp = User(
                            userId = "emp_" + UUID.randomUUID().toString().take(8),
                            name = name.trim(),
                            email = email.trim(),
                            mobile = mobile.trim(),
                            state = state.trim().ifBlank { "Rajasthan" },
                            district = district.trim(),
                            role = UserRole.EMPLOYEE,
                            status = UserStatus.ACTIVE
                        )
                        onSaveEmployee(newEmp) { result ->
                            isSaving = false
                            if (result.isSuccess) {
                                showAddEmployeeDialog = false
                            } else {
                                addErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create officer."
                            }
                        }
                    },
                    enabled = !isSaving && name.isNotBlank() && email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Officer")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddEmployeeDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Employee Dialog
    if (employeeToEdit != null) {
        val emp = employeeToEdit!!
        var name by remember { mutableStateOf(emp.name) }
        var email by remember { mutableStateOf(emp.email) }
        var mobile by remember { mutableStateOf(emp.mobile) }
        var state by remember { mutableStateOf(emp.state.ifBlank { "Rajasthan" }) }
        var district by remember { mutableStateOf(emp.district) }
        var status by remember { mutableStateOf(emp.status) }
        var isSaving by remember { mutableStateOf(false) }
        var editErrorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) employeeToEdit = null },
            title = { Text("Edit Officer Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editErrorMessage != null) {
                        Surface(
                            color = Red600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = editErrorMessage!!,
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            editErrorMessage = null
                        },
                        label = { Text("Full Name", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            editErrorMessage = null
                        },
                        label = { Text("Email Address", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Account Status (सक्रिय/निष्क्रिय)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = status == UserStatus.ACTIVE,
                            onCheckedChange = { checked -> status = if (checked) UserStatus.ACTIVE else UserStatus.INACTIVE },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Indigo600
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        editErrorMessage = null
                        val updated = emp.copy(
                            name = name.trim(),
                            email = email.trim(),
                            mobile = mobile.trim(),
                            state = state.trim(),
                            district = district.trim(),
                            status = status
                        )
                        onSaveEmployee(updated) { result ->
                            isSaving = false
                            if (result.isSuccess) {
                                employeeToEdit = null
                            } else {
                                editErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update officer."
                            }
                        }
                    },
                    enabled = !isSaving && name.isNotBlank() && email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { employeeToEdit = null },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
        Text(value.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy900)
    }
}

@Composable
fun CompactEmployeeCardItem(
    employee: User,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleStatus: (UserStatus) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Name & State/District
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${employee.state.ifBlank { "Rajasthan" }} • ${employee.district.ifBlank { "All Districts" }}",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            // Right Row: Status Dot (Green for Active, Red for Inactive) + Edit Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Indicator Dot: Green for Active, Red for Inactive
                val isActive = employee.status == UserStatus.ACTIVE
                val dotColor = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444)
                val dotBg = if (isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(dotBg)
                        .clickable {
                            onToggleStatus(if (isActive) UserStatus.INACTIVE else UserStatus.ACTIVE)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }

                // Edit Button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Officer",
                        tint = Indigo600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
