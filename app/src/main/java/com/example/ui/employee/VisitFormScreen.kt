package com.example.ui.employee

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PhotoCategory
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.data.model.VisitStatus
import com.example.ui.components.SyncStatusBanner
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitFormScreen(
    employeeUser: User,
    task: Task?,
    initialSchool: School?,
    isOnline: Boolean,
    pendingSyncCount: Int,
    onBackClick: () -> Unit,
    onSubmitVisit: (Visit, (Result<Unit>) -> Unit) -> Unit,
    onUpdateSchoolInfo: (School) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 5

    // School Details State
    var schoolName by remember { mutableStateOf(task?.schoolName ?: initialSchool?.schoolName ?: "") }
    var referenceCode by remember { mutableStateOf(initialSchool?.referenceCode ?: "") }
    var district by remember { mutableStateOf(task?.district ?: initialSchool?.district ?: "") }
    var block by remember { mutableStateOf(task?.block ?: initialSchool?.block ?: "") }
    var principalName by remember { mutableStateOf(initialSchool?.principalName ?: "") }
    var principalMobile by remember { mutableStateOf(initialSchool?.mobile ?: "") }
    var visitDate by remember { mutableStateOf(task?.visitDate ?: "13-Aug-2026") }

    // Questionnaire Answers
    var metPrincipal by remember { mutableStateOf("हाँ") }
    var missionGyanAwareness by remember { mutableStateOf("हाँ") }
    var studentCount by remember { mutableStateOf("120") }
    var schoolResponse by remember { mutableStateOf("बहुत अच्छी") }
    var bciContactDetails by remember { mutableStateOf("") }
    var whatsappGroupAdded by remember { mutableStateOf("हाँ") }
    var posterInstalled by remember { mutableStateOf("हाँ") }
    var keyObservations by remember { mutableStateOf("") }
    var problemsOrAssistance by remember { mutableStateOf("") }
    var followupRequired by remember { mutableStateOf("नहीं") }
    var finalRemarks by remember { mutableStateOf("") }
    var smartClassStatus by remember { mutableStateOf("बहुत अच्छी") }

    // Photo Map Category ID -> List of Uri Strings
    val photoMap = remember {
        mutableStateMapOf<String, MutableList<String>>().apply {
            PhotoCategory.entries.forEach { put(it.categoryId, mutableListOf()) }
        }
    }

    var showEditSchoolDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var activePhotoCategory by remember { mutableStateOf<PhotoCategory?>(null) }

    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        activePhotoCategory?.let { cat ->
            val list = photoMap[cat.categoryId] ?: mutableListOf()
            uris.forEach { uri -> list.add(uri.toString()) }
            photoMap[cat.categoryId] = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SOE School Visit Form", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(schoolName, fontSize = 12.sp, color = Slate500, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            SyncStatusBanner(
                isOnline = isOnline,
                pendingCount = pendingSyncCount,
                onSyncClick = {}
            )

            // Step Bar Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (currentStep) {
                                1 -> "Step 1: School & Principal Details"
                                2 -> "Step 2: App Awareness & Engagement"
                                3 -> "Step 3: Operations & Smart Class"
                                4 -> "Step 4: Observations & Follow-up"
                                else -> "Step 5: Photo Uploads (Mandatory)"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600
                        )
                        Text(
                            text = "Step $currentStep of $totalSteps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 1..totalSteps) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(if (i <= currentStep) Indigo600 else Slate200)
                            )
                        }
                    }
                }
            }

            // Scrollable Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (submitError != null) {
                    Text(
                        text = submitError!!,
                        color = Red600,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (currentStep) {
                    1 -> {
                        // STEP 1: School Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("School Information", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                    IconButton(onClick = { showEditSchoolDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Correct Info", tint = Indigo600)
                                    }
                                }

                                Text("Verify auto-filled details. Click pencil icon if updates are needed.", fontSize = 12.sp, color = Slate500)

                                Spacer(modifier = Modifier.height(14.dp))

                                DetailRow(label = "School Name", value = schoolName)
                                DetailRow(label = "Reference Code", value = referenceCode.ifBlank { "Not specified" })
                                DetailRow(label = "District", value = district)
                                DetailRow(label = "Block", value = block)
                                DetailRow(label = "Principal Name", value = principalName.ifBlank { "Not specified" })
                                DetailRow(label = "Principal Mobile", value = principalMobile.ifBlank { "Not specified" })
                                DetailRow(label = "Visit Date", value = visitDate)
                            }
                        }

                        // Q9: Met Principal Sir?
                        SingleChoiceQuestion(
                            question = "9. प्रधानाचार्य महोदय से मुलाकात हुई? (Met Principal Sir?)",
                            options = listOf("हाँ", "नहीं"),
                            selectedOption = metPrincipal,
                            onOptionSelected = { metPrincipal = it }
                        )
                    }

                    2 -> {
                        // STEP 2: App Awareness & Engagement
                        SingleChoiceQuestion(
                            question = "10. Mission Gyan App के बारे में जानकारी? (App Knowledge?)",
                            options = listOf("हाँ", "नहीं", "थोड़ी जानकारी थी"),
                            selectedOption = missionGyanAwareness,
                            onOptionSelected = { missionGyanAwareness = it }
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("11. उपस्थित विद्यार्थियों की संख्या (Student Attendance)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = studentCount,
                                    onValueChange = { studentCount = it },
                                    placeholder = { Text("e.g. 120") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        SingleChoiceQuestion(
                            question = "12. विद्यालय की प्रतिक्रिया (School Response)",
                            options = listOf("बहुत अच्छी", "अच्छी", "सामान्य", "कमजोर"),
                            selectedOption = schoolResponse,
                            onOptionSelected = { schoolResponse = it }
                        )
                    }

                    3 -> {
                        // STEP 3: Operations & Smart Class
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("13. BCI संपर्क विवरण (BCI Contact Details)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = bciContactDetails,
                                    onValueChange = { bciContactDetails = it },
                                    placeholder = { Text("Name, Designation & Contact Number") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        SingleChoiceQuestion(
                            question = "14. विद्यालय/SMC WhatsApp समूह में जोड़े गए? (Added in WhatsApp Group?)",
                            options = listOf("हाँ", "नहीं", "लंबित"),
                            selectedOption = whatsappGroupAdded,
                            onOptionSelected = { whatsappGroupAdded = it }
                        )

                        SingleChoiceQuestion(
                            question = "15. पोस्टर लगाया गया? (Poster Installed?)",
                            options = listOf("हाँ", "नहीं"),
                            selectedOption = posterInstalled,
                            onOptionSelected = { posterInstalled = it }
                        )

                        SingleChoiceQuestion(
                            question = "21. स्मार्ट क्लास की स्थिति (Smart Class Status)",
                            options = listOf("बहुत अच्छी", "अच्छी", "सामान्य", "खराब", "उपयोग में नहीं है", "स्मार्ट क्लास उपलब्ध नहीं है"),
                            selectedOption = smartClassStatus,
                            onOptionSelected = { smartClassStatus = it }
                        )
                    }

                    4 -> {
                        // STEP 4: Observations & Follow-up
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("16. मुख्य अवलोकन (Key Observations)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = keyObservations,
                                    onValueChange = { keyObservations = it },
                                    placeholder = { Text("Write key observations during school visit...") },
                                    minLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("17. समस्याएं / सहायता आवश्यकता (Problems/Assistance Needed)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = problemsOrAssistance,
                                    onValueChange = { problemsOrAssistance = it },
                                    placeholder = { Text("Describe any problems faced or support needed...") },
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        SingleChoiceQuestion(
                            question = "18. फॉलो-अप आवश्यक है? (Follow-up Required?)",
                            options = listOf("हाँ", "नहीं"),
                            selectedOption = followupRequired,
                            onOptionSelected = { followupRequired = it }
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("20. अंतिम टिप्पणी (Final Remarks)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = finalRemarks,
                                    onValueChange = { finalRemarks = it },
                                    placeholder = { Text("Final remarks / overall assessment...") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    5 -> {
                        // STEP 5: Photo Uploads
                        Text("19. फोटो अपलोड (Upload Photos - 5 Mandatory Categories)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)

                        PhotoCategory.entries.forEach { category ->
                            val currentList = photoMap[category.categoryId] ?: emptyList()
                            val isSatisfied = if (category.minRequired > 0) currentList.size >= category.minRequired else true

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(category.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                            Text(
                                                text = if (category.minRequired > 0) "Mandatory (Min ${category.minRequired} required)" else "Optional",
                                                fontSize = 11.sp,
                                                color = if (isSatisfied) Emerald600 else Red600
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                activePhotoCategory = category
                                                photoPickerLauncher.launch("image/*")
                                            }
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = "Add Photo", tint = Indigo600)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (currentList.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(80.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .clickable {
                                                    activePhotoCategory = category
                                                    photoPickerLauncher.launch("image/*")
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Slate500)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Tap to capture / upload photo", fontSize = 13.sp, color = Slate500)
                                            }
                                        }
                                    } else {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(currentList) { uriStr ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = uriStr,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Back", fontWeight = FontWeight.Bold, color = Slate700)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                // Final Submission logic
                                isSubmitting = true
                                submitError = null

                                val answers = VisitAnswers(
                                    q1_soeName = employeeUser.name,
                                    q2_visitDate = visitDate,
                                    q3_schoolName = schoolName,
                                    q4_udiseCode = referenceCode,
                                    q5_district = district,
                                    q6_block = block,
                                    q7_principalName = principalName,
                                    q8_principalMobile = principalMobile,
                                    q9_metPrincipal = metPrincipal,
                                    q10_missionGyanAwareness = missionGyanAwareness,
                                    q11_studentCount = studentCount,
                                    q12_schoolResponse = schoolResponse,
                                    q13_bciContactDetails = bciContactDetails,
                                    q14_whatsappGroupAdded = whatsappGroupAdded,
                                    q15_posterInstalled = posterInstalled,
                                    q16_keyObservations = keyObservations,
                                    q17_problemsOrAssistance = problemsOrAssistance,
                                    q18_followupRequired = followupRequired,
                                    q20_finalRemarks = finalRemarks,
                                    q21_smartClassStatus = smartClassStatus
                                )

                                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                                val answersAdapter = moshi.adapter(VisitAnswers::class.java)
                                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
                                val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

                                val visitId = task?.visitId ?: "vst_" + UUID.randomUUID().toString().take(8)
                                val schoolId = task?.schoolId ?: initialSchool?.schoolId ?: "sch_" + UUID.randomUUID().toString().take(8)

                                val finalVisit = Visit(
                                    visitId = visitId,
                                    schoolId = schoolId,
                                    employeeId = employeeUser.userId,
                                    employeeName = employeeUser.name,
                                    schoolName = schoolName,
                                    district = district,
                                    block = block,
                                    visitDate = visitDate,
                                    status = VisitStatus.SUBMITTED,
                                    answersJson = answersAdapter.toJson(answers),
                                    photosJson = photosAdapter.toJson(photoMap.mapValues { it.value.toList() })
                                )

                                onSubmitVisit(finalVisit) { res ->
                                    isSubmitting = false
                                    if (res.isSuccess) {
                                        onBackClick()
                                    } else {
                                        submitError = res.exceptionOrNull()?.localizedMessage ?: "Failed to submit visit report"
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = if (currentStep == totalSteps) "Submit Report" else "Next Step",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Correct School Info Dialog
    if (showEditSchoolDialog) {
        AlertDialog(
            onDismissRequest = { showEditSchoolDialog = false },
            title = { Text("Correct School Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("School Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = referenceCode,
                        onValueChange = { referenceCode = it },
                        label = { Text("Reference Code") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("District") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = block,
                        onValueChange = { block = it },
                        label = { Text("Block") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = principalName,
                        onValueChange = { principalName = it },
                        label = { Text("Principal Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = principalMobile,
                        onValueChange = { principalMobile = it },
                        label = { Text("Principal Mobile") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditSchoolDialog = false
                        initialSchool?.let {
                            onUpdateSchoolInfo(
                                it.copy(
                                    schoolName = schoolName,
                                    referenceCode = referenceCode,
                                    district = district,
                                    block = block,
                                    principalName = principalName,
                                    mobile = principalMobile
                                )
                            )
                        }
                    }
                ) {
                    Text("Save & Update Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSchoolDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SingleChoiceQuestion(
    question: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = question, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
            Spacer(modifier = Modifier.height(10.dp))

            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOptionSelected(option) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == option,
                        onClick = { onOptionSelected(option) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = option, fontSize = 14.sp, color = Slate700, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Slate500)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
    }
}
