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
                    it.district.contains(searchQuery, ignoreCase = true) ||
                    it.block.contains(searchQuery, ignoreCase = true) ||
                    it.referenceCode.contains(searchQuery, ignoreCase = true)
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddSchoolDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Indigo600)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add School", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo600)
                        }

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                placeholder = "Search by school name, ref code, district, block..."
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

    // Edit School Dialog
    if (selectedSchoolForEdit != null) {
        val sch = selectedSchoolForEdit!!
        var eName by remember { mutableStateOf(sch.schoolName) }
        var eRefCode by remember { mutableStateOf(sch.referenceCode) }
        var eDistrict by remember { mutableStateOf(sch.district) }
        var eBlock by remember { mutableStateOf(sch.block) }
        var ePrincipal by remember { mutableStateOf(sch.principalName) }
        var eMobile by remember { mutableStateOf(sch.mobile) }

        AlertDialog(
            onDismissRequest = { selectedSchoolForEdit = null },
            title = { Text("Edit School Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = eName, onValueChange = { eName = it }, label = { Text("School Name") }, singleLine = true)
                    OutlinedTextField(value = eRefCode, onValueChange = { eRefCode = it }, label = { Text("Reference Code") }, singleLine = true)
                    OutlinedTextField(value = eDistrict, onValueChange = { eDistrict = it }, label = { Text("District") }, singleLine = true)
                    OutlinedTextField(value = eBlock, onValueChange = { eBlock = it }, label = { Text("Block") }, singleLine = true)
                    OutlinedTextField(value = ePrincipal, onValueChange = { ePrincipal = it }, label = { Text("Principal Name") }, singleLine = true)
                    OutlinedTextField(value = eMobile, onValueChange = { eMobile = it }, label = { Text("Principal Mobile") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSchool(
                            sch.copy(
                                schoolName = eName,
                                referenceCode = eRefCode,
                                district = eDistrict,
                                block = eBlock,
                                principalName = ePrincipal,
                                mobile = eMobile
                            )
                        )
                        selectedSchoolForEdit = null
                    }
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

    // Manual Add School Dialog
    if (showAddSchoolDialog) {
        var mName by remember { mutableStateOf("") }
        var mRefCode by remember { mutableStateOf("") }
        var mState by remember { mutableStateOf("Rajasthan") }
        var mDistrict by remember { mutableStateOf("") }
        var mBlock by remember { mutableStateOf("") }
        var mVillage by remember { mutableStateOf("") }
        var mType by remember { mutableStateOf("Senior Secondary") }
        var mPrincipal by remember { mutableStateOf("") }
        var mMobile by remember { mutableStateOf("") }
        var mError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddSchoolDialog = false },
            title = { Text("मैन्युअल स्कूल जोड़ें (Add School)", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (mError != null) {
                        Text(mError!!, color = Red600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = mName,
                        onValueChange = { mName = it },
                        label = { Text("School Name (स्कूल का नाम) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mRefCode,
                        onValueChange = { mRefCode = it },
                        label = { Text("UDISE / Reference Code (यू-डाइस कोड)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mDistrict,
                            onValueChange = { mDistrict = it },
                            label = { Text("District (जिला) *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mBlock,
                            onValueChange = { mBlock = it },
                            label = { Text("Block (ब्लॉक) *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mVillage,
                            onValueChange = { mVillage = it },
                            label = { Text("Village (गांव)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mType,
                            onValueChange = { mType = it },
                            label = { Text("Type (प्रकार)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = mPrincipal,
                        onValueChange = { mPrincipal = it },
                        label = { Text("Principal Name (प्रधानाचार्य)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mMobile,
                        onValueChange = { mMobile = it },
                        label = { Text("Principal Mobile (मोबाइल)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mName.isBlank()) {
                            mError = "School Name is required!"
                            return@Button
                        }
                        val newSchool = School(
                            schoolId = "sch_" + java.util.UUID.randomUUID().toString().take(8),
                            schoolName = mName.trim(),
                            referenceCode = mRefCode.trim(),
                            state = mState.trim().ifBlank { "Rajasthan" },
                            district = mDistrict.trim(),
                            block = mBlock.trim(),
                            village = mVillage.trim(),
                            type = mType.trim(),
                            principalName = mPrincipal.trim(),
                            mobile = mMobile.trim(),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = school.schoolName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit School", tint = Indigo600)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Block: ${school.block} • District: ${school.district}",
                fontSize = 12.sp,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Principal: ${school.principalName.ifBlank { "N/A" }}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate700)
                
                val context = LocalContext.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (school.mobile.isNotBlank() && school.mobile != "N/A") {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${school.mobile}"))
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call Principal", tint = Indigo600, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = school.mobile.ifBlank { "N/A" },
                        fontSize = 12.sp,
                        color = if (school.mobile.isNotBlank() && school.mobile != "N/A") Indigo600 else Slate700,
                        fontWeight = if (school.mobile.isNotBlank() && school.mobile != "N/A") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
