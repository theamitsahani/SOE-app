package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
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
import com.example.data.model.School
import com.example.ui.components.SearchTextField
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.ExcelHelper
import com.example.util.ImportValidationResult

@Composable
fun SchoolManagementTab(
    schools: List<School>,
    onImportSchools: (List<School>, List<com.example.data.model.Visit>, (Result<Int>) -> Unit) -> Unit,
    onUpdateSchool: (School) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSchoolForEdit by remember { mutableStateOf<School?>(null) }
    var showAddSchoolDialog by remember { mutableStateOf(false) }
    var importValidationResult by remember { mutableStateOf<ImportValidationResult?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = ExcelHelper.parseSchoolCsv(context, uri, schools)
            importValidationResult = result
        }
    }

    val filteredSchools = remember(schools, searchQuery) {
        if (searchQuery.isBlank()) schools
        else schools.filter {
            it.schoolName.contains(searchQuery, ignoreCase = true) ||
                    it.districtName.contains(searchQuery, ignoreCase = true) ||
                    it.blockName.contains(searchQuery, ignoreCase = true) ||
                    it.villageName.contains(searchQuery, ignoreCase = true) ||
                    it.principalName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header with Actions (Manual Add + Import Excel)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("School Directory", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Text("Total: ${schools.size} schools enrolled", fontSize = 12.sp, color = Slate500)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showAddSchoolDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Indigo600)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Indigo600)
                        }

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Import Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            SearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search by school name, district, block, village..."
            )
        }

        if (filteredSchools.isEmpty()) {
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
                        Text("No schools found matching search", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate500)
                    }
                }
            }
        } else {
            items(filteredSchools) { school ->
                SchoolCardItem(
                    school = school,
                    onEditClick = { selectedSchoolForEdit = school }
                )
            }
        }
    }

    // Edit School Dialog - 9 Fields in Exact Order
    if (selectedSchoolForEdit != null) {
        val sch = selectedSchoolForEdit!!
        var eStateName by remember { mutableStateOf(sch.stateName) }
        var eDistrictName by remember { mutableStateOf(sch.districtName) }
        var eSchoolName by remember { mutableStateOf(sch.schoolName) }
        var eSchoolType by remember { mutableStateOf(sch.schoolType) }
        var eVillageName by remember { mutableStateOf(sch.villageName) }
        var ePrincipalName by remember { mutableStateOf(sch.principalName) }
        var eBlockName by remember { mutableStateOf(sch.blockName) }
        var ePrincipalMobile by remember { mutableStateOf(sch.principalMobile) }
        var eVisitDate by remember { mutableStateOf(sch.visitDate) }

        AlertDialog(
            onDismissRequest = { selectedSchoolForEdit = null },
            title = { Text("Edit School Record (स्कूल संपादित करें)", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. State Name
                    OutlinedTextField(
                        value = eStateName,
                        onValueChange = { eStateName = it },
                        label = { Text("1. State Name (राज्य का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 2. District Name
                    OutlinedTextField(
                        value = eDistrictName,
                        onValueChange = { eDistrictName = it },
                        label = { Text("2. District Name (जिले का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 3. School Name
                    OutlinedTextField(
                        value = eSchoolName,
                        onValueChange = { eSchoolName = it },
                        label = { Text("3. School Name (स्कूल का नाम) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 4. School Type
                    OutlinedTextField(
                        value = eSchoolType,
                        onValueChange = { eSchoolType = it },
                        label = { Text("4. School Type (स्कूल का प्रकार)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 5. Village Name
                    OutlinedTextField(
                        value = eVillageName,
                        onValueChange = { eVillageName = it },
                        label = { Text("5. Village Name (गांव का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 6. Principal Name
                    OutlinedTextField(
                        value = ePrincipalName,
                        onValueChange = { ePrincipalName = it },
                        label = { Text("6. Principal Name (प्रधानाचार्य का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 7. Block Name
                    OutlinedTextField(
                        value = eBlockName,
                        onValueChange = { eBlockName = it },
                        label = { Text("7. Block Name (ब्लॉक का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 8. Principal Mobile Number
                    OutlinedTextField(
                        value = ePrincipalMobile,
                        onValueChange = { ePrincipalMobile = it },
                        label = { Text("8. Principal Mobile Number (मोबाइल नंबर)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 9. Visit Date
                    OutlinedTextField(
                        value = eVisitDate,
                        onValueChange = { eVisitDate = it },
                        label = { Text("9. Visit Date (विज़िट तिथि)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSchool(
                            sch.copy(
                                stateName = eStateName,
                                districtName = eDistrictName,
                                schoolName = eSchoolName,
                                schoolType = eSchoolType,
                                villageName = eVillageName,
                                principalName = ePrincipalName,
                                blockName = eBlockName,
                                principalMobile = ePrincipalMobile,
                                visitDate = eVisitDate
                            )
                        )
                        selectedSchoolForEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSchoolForEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual Add School Dialog - 9 Fields in Exact Order
    if (showAddSchoolDialog) {
        var mStateName by remember { mutableStateOf("Rajasthan") }
        var mDistrictName by remember { mutableStateOf("") }
        var mSchoolName by remember { mutableStateOf("") }
        var mSchoolType by remember { mutableStateOf("Senior Secondary") }
        var mVillageName by remember { mutableStateOf("") }
        var mPrincipalName by remember { mutableStateOf("") }
        var mBlockName by remember { mutableStateOf("") }
        var mPrincipalMobile by remember { mutableStateOf("") }
        var mVisitDate by remember { mutableStateOf("") }
        var mError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddSchoolDialog = false },
            title = { Text("Add School Record (स्कूल जोड़ें)", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (mError != null) {
                        Text(mError!!, color = Red600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // 1. State Name
                    OutlinedTextField(
                        value = mStateName,
                        onValueChange = { mStateName = it },
                        label = { Text("1. State Name (राज्य का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 2. District Name
                    OutlinedTextField(
                        value = mDistrictName,
                        onValueChange = { mDistrictName = it },
                        label = { Text("2. District Name (जिले का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 3. School Name
                    OutlinedTextField(
                        value = mSchoolName,
                        onValueChange = { mSchoolName = it },
                        label = { Text("3. School Name (स्कूल का नाम) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 4. School Type
                    OutlinedTextField(
                        value = mSchoolType,
                        onValueChange = { mSchoolType = it },
                        label = { Text("4. School Type (प्रकार)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 5. Village Name
                    OutlinedTextField(
                        value = mVillageName,
                        onValueChange = { mVillageName = it },
                        label = { Text("5. Village Name (गांव का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 6. Principal Name
                    OutlinedTextField(
                        value = mPrincipalName,
                        onValueChange = { mPrincipalName = it },
                        label = { Text("6. Principal Name (प्रधानाचार्य का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 7. Block Name
                    OutlinedTextField(
                        value = mBlockName,
                        onValueChange = { mBlockName = it },
                        label = { Text("7. Block Name (ब्लॉक का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 8. Principal Mobile Number
                    OutlinedTextField(
                        value = mPrincipalMobile,
                        onValueChange = { mPrincipalMobile = it },
                        label = { Text("8. Principal Mobile Number (मोबाइल नंबर)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 9. Visit Date
                    OutlinedTextField(
                        value = mVisitDate,
                        onValueChange = { mVisitDate = it },
                        label = { Text("9. Visit Date (विज़िट तिथि)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mSchoolName.isBlank()) {
                            mError = "School Name is required!"
                            return@Button
                        }
                        val newSchool = School(
                            schoolId = "sch_" + java.util.UUID.randomUUID().toString().take(8),
                            stateName = mStateName.trim().ifBlank { "Rajasthan" },
                            districtName = mDistrictName.trim(),
                            schoolName = mSchoolName.trim(),
                            schoolType = mSchoolType.trim(),
                            villageName = mVillageName.trim(),
                            principalName = mPrincipalName.trim(),
                            blockName = mBlockName.trim(),
                            principalMobile = mPrincipalMobile.trim(),
                            visitDate = mVisitDate.trim(),
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onImportSchools(listOf(newSchool), emptyList()) {
                            showAddSchoolDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Save School")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSchoolDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Excel Preview & Validation Dialog
    if (importValidationResult != null) {
        val res = importValidationResult!!

        AlertDialog(
            onDismissRequest = { importValidationResult = null },
            title = { Text("Excel Import Validation Summary", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("• Total File Rows: ${res.totalRows}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("• Valid Rows to Import: ${res.validRows}", color = Emerald600, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("• Duplicates Flagged: ${res.duplicateRows}", color = Slate700, fontSize = 13.sp)
                            Text("• Invalid / Skipped Rows: ${res.invalidRows}", color = Red600, fontSize = 13.sp)
                            if (res.completedVisitsToImport.isNotEmpty()) {
                                Text("• Completed Visits Flagged (Col I/J): ${res.completedVisitsToImport.size}", color = Indigo600, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    if (res.errors.isNotEmpty()) {
                        Text("Skipped Row Details:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red600)
                        res.errors.take(5).forEach { err ->
                            Text("• $err", fontSize = 11.sp, color = Slate700)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImporting = true
                        onImportSchools(res.schoolsToImport, res.completedVisitsToImport) {
                            isImporting = false
                            importValidationResult = null
                        }
                    },
                    enabled = !isImporting && res.schoolsToImport.isNotEmpty()
                ) {
                    Text("Confirm Import (${res.schoolsToImport.size} Schools)")
                }
            },
            dismissButton = {
                TextButton(onClick = { importValidationResult = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SchoolCardItem(
    school: School,
    onEditClick: () -> Unit
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = school.schoolName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    if (school.schoolType.isNotBlank()) {
                        Text(
                            text = school.schoolType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Indigo600
                        )
                    }
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit School", tint = Indigo600)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val locationText = listOfNotNull(
                school.blockName.takeIf { it.isNotBlank() }?.let { "Block: $it" },
                school.villageName.takeIf { it.isNotBlank() }?.let { "Village: $it" },
                school.districtName.takeIf { it.isNotBlank() }?.let { "District: $it" },
                school.stateName.takeIf { it.isNotBlank() && it != "Rajasthan" }?.let { "State: $it" }
            ).joinToString(" • ")

            if (locationText.isNotBlank()) {
                Text(
                    text = locationText,
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Principal: ${school.principalName.ifBlank { "N/A" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700
                )

                val context = LocalContext.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (school.principalMobile.isNotBlank() && school.principalMobile != "N/A") {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${school.principalMobile}"))
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call Principal", tint = Indigo600, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = school.principalMobile.ifBlank { "N/A" },
                        fontSize = 12.sp,
                        color = if (school.principalMobile.isNotBlank() && school.principalMobile != "N/A") Indigo600 else Slate700,
                        fontWeight = if (school.principalMobile.isNotBlank() && school.principalMobile != "N/A") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            if (school.visitDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Visit Date: ${school.visitDate}",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
    }
}
