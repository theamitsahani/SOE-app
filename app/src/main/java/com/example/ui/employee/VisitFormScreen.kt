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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.MediaStorageHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitFormScreen(
    employeeUser: User,
    task: Task?,
    initialSchool: School?,
    existingVisit: Visit? = null,
    isOnline: Boolean,
    pendingSyncCount: Int,
    onBackClick: () -> Unit,
    onSubmitVisit: (Visit, (Result<Unit>) -> Unit) -> Unit,
    onUpdateSchoolInfo: (School) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 5

    // Parse existing answers if editing
    val parsedExistingAnswers = remember(existingVisit) {
        if (existingVisit != null && existingVisit.answersJson.isNotBlank()) {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                moshi.adapter(VisitAnswers::class.java).fromJson(existingVisit.answersJson) ?: VisitAnswers()
            } catch (e: Exception) {
                VisitAnswers()
            }
        } else {
            VisitAnswers()
        }
    }

    // Parse existing photos if editing
    val parsedExistingPhotos = remember(existingVisit) {
        if (existingVisit != null && existingVisit.photosJson.isNotBlank()) {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
                val adapter = moshi.adapter<Map<String, List<String>>>(mapType)
                adapter.fromJson(existingVisit.photosJson) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    // School Details State
    var schoolName by remember { mutableStateOf(existingVisit?.schoolName?.ifBlank { null } ?: task?.schoolName ?: initialSchool?.schoolName ?: "") }
    var udiseCode by remember { mutableStateOf(parsedExistingAnswers.q4_udiseCode.ifBlank { initialSchool?.referenceCode ?: "" }) }
    var stateName by remember { mutableStateOf(existingVisit?.state?.ifBlank { null } ?: initialSchool?.state ?: "Rajasthan") }
    var district by remember { mutableStateOf(existingVisit?.district?.ifBlank { null } ?: task?.district ?: initialSchool?.district ?: "") }
    var block by remember { mutableStateOf(existingVisit?.block?.ifBlank { null } ?: task?.block ?: initialSchool?.block ?: "") }
    var principalName by remember { mutableStateOf(parsedExistingAnswers.q7_principalName.ifBlank { initialSchool?.principalName ?: "" }) }
    var principalMobile by remember { mutableStateOf(parsedExistingAnswers.q8_principalMobile.ifBlank { initialSchool?.principalMobile?.ifBlank { initialSchool.mobile } ?: "" }) }
    var visitDate by remember { mutableStateOf(existingVisit?.visitDate?.ifBlank { null } ?: task?.visitDate ?: "14-Aug-2026") }

    // Participating Classes Checkboxes (Class 6th to 12th) - Unchecked by default for new visit
    val availableClasses = remember { listOf("Class 6th", "Class 7th", "Class 8th", "Class 9th", "Class 10th", "Class 11th", "Class 12th") }
    var selectedClasses by remember {
        val initialSelected = if (parsedExistingAnswers.q22_participatingClasses.isNotBlank()) {
            parsedExistingAnswers.q22_participatingClasses.split(",").map { it.trim() }.toSet()
        } else {
            emptySet()
        }
        mutableStateOf(initialSelected)
    }

    // Questionnaire Answers - Unselected / empty by default for new visit
    var metPrincipal by remember { mutableStateOf(parsedExistingAnswers.q9_metPrincipal) }
    var missionGyanAwareness by remember { mutableStateOf(parsedExistingAnswers.q10_missionGyanAwareness) }
    var studentCount by remember { mutableStateOf(parsedExistingAnswers.q11_studentCount) }
    var schoolResponse by remember { mutableStateOf(parsedExistingAnswers.q12_schoolResponse) }

    // Point 13: BCI Name and Contact Number
    var bciName by remember {
        val initialBciName = parsedExistingAnswers.q13_bciName.ifBlank {
            if (parsedExistingAnswers.q13_bciContactDetails.contains("-")) parsedExistingAnswers.q13_bciContactDetails.substringBefore("-").trim()
            else parsedExistingAnswers.q13_bciContactDetails
        }
        mutableStateOf(initialBciName)
    }
    var bciMobile by remember {
        val initialBciMobile = parsedExistingAnswers.q13_bciMobile.ifBlank {
            if (parsedExistingAnswers.q13_bciContactDetails.contains("-")) parsedExistingAnswers.q13_bciContactDetails.substringAfter("-").trim()
            else ""
        }
        mutableStateOf(initialBciMobile)
    }

    var whatsappGroupAdded by remember { mutableStateOf(parsedExistingAnswers.q14_whatsappGroupAdded) }
    var posterInstalled by remember { mutableStateOf(parsedExistingAnswers.q15_posterInstalled) }
    var keyObservations by remember { mutableStateOf(parsedExistingAnswers.q16_keyObservations) }
    var problemsOrAssistance by remember { mutableStateOf(parsedExistingAnswers.q17_problemsOrAssistance) }
    var followupRequired by remember { mutableStateOf(parsedExistingAnswers.q18_followupRequired) }
    var finalRemarks by remember { mutableStateOf(parsedExistingAnswers.q20_finalRemarks) }
    var smartClassStatus by remember { mutableStateOf(parsedExistingAnswers.q21_smartClassStatus) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Photo Map Category ID -> List of Uri Strings
    val photoMap = remember {
        mutableStateMapOf<String, MutableList<String>>().apply {
            PhotoCategory.entries.forEach { cat ->
                val existingList = parsedExistingPhotos[cat.categoryId]?.toMutableList() ?: mutableListOf()
                put(cat.categoryId, existingList)
            }
        }
    }

    var showEditSchoolDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var showSubmissionSuccessDialog by remember { mutableStateOf(false) }
    var submissionSuccessMessage by remember { mutableStateOf("") }
    var activePhotoCategory by remember { mutableStateOf<PhotoCategory?>(null) }
    var previewMediaUrl by remember { mutableStateOf<String?>(null) }

    // Multi-Photo Picker (Unlimited)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        activePhotoCategory?.let { cat ->
            coroutineScope.launch {
                val list = (photoMap[cat.categoryId] ?: mutableListOf()).toMutableList()
                uris.forEach { uri ->
                    val savedLocalUri = MediaStorageHelper.saveMediaLocally(context, uri)
                    list.add(savedLocalUri)
                }
                photoMap[cat.categoryId] = list
            }
        }
    }

    // Multi-Video Picker (Unlimited)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        activePhotoCategory?.let { cat ->
            coroutineScope.launch {
                val list = (photoMap[cat.categoryId] ?: mutableListOf()).toMutableList()
                uris.forEach { uri ->
                    val savedLocalUri = MediaStorageHelper.saveMediaLocally(context, uri)
                    list.add(savedLocalUri)
                }
                photoMap[cat.categoryId] = list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (existingVisit != null) "Edit Visit Report (रिपोर्ट संशोधन)" else "SOE School Visit Form",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                        // STEP 1: School Details & UDISE Code
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
                                DetailRow(label = "State", value = stateName)
                                DetailRow(label = "District", value = district)
                                DetailRow(label = "Block", value = block)
                                DetailRow(label = "Principal Name", value = principalName.ifBlank { "Not specified" })
                                DetailRow(label = "Principal Mobile", value = principalMobile.ifBlank { "Not specified" })
                                DetailRow(label = "Visit Date", value = visitDate)
                            }
                        }

                        // UDISE Code Input Field
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("UDISE Code (यू-डाइस कोड)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = udiseCode,
                                    onValueChange = { udiseCode = it },
                                    placeholder = { Text("Enter 11-digit UDISE Code (e.g. 08010100101)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                        // STEP 2: App Awareness, Attendance & Participating Classes
                        SingleChoiceQuestion(
                            question = "10. Mission Gyan App के बारे में जानकारी? (App Knowledge?)",
                            options = listOf("हाँ", "नहीं", "थोड़ी जानकारी थी"),
                            selectedOption = missionGyanAwareness,
                            onOptionSelected = { missionGyanAwareness = it }
                        )

                        // Participating Classes Checkboxes (Class 6th to 12th)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("भाग लेने वाली कक्षाएं (Participating Classes 6th - 12th)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Text("जिन कक्षाओं ने कार्यक्रम/विज़िट में भाग लिया उन्हें चुनें:", fontSize = 12.sp, color = Slate500)
                                Spacer(modifier = Modifier.height(10.dp))

                                availableClasses.forEach { cls ->
                                    val isChecked = selectedClasses.contains(cls)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedClasses = if (isChecked) selectedClasses - cls else selectedClasses + cls
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedClasses = if (checked == true) selectedClasses + cls else selectedClasses - cls
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Indigo600)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cls, fontSize = 14.sp, color = Slate700, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

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
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "13. BCI संपर्क विवरण (BCI Details)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )

                                // Field 1: BCI Officer Name
                                OutlinedTextField(
                                    value = bciName,
                                    onValueChange = { bciName = it },
                                    label = { Text("BCI Officer Name (BCI का नाम)", fontSize = 12.sp) },
                                    placeholder = { Text("Enter BCI Officer Name") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Field 2: BCI Contact Mobile Number
                                OutlinedTextField(
                                    value = bciMobile,
                                    onValueChange = { bciMobile = it },
                                    label = { Text("BCI Contact Number (BCI मोबाइल नंबर)", fontSize = 12.sp) },
                                    placeholder = { Text("10-digit mobile number") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
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
                        // STEP 5: Photo & Video Uploads
                        Text("19. फोटो व वीडियो अपलोड (Upload Photos & Videos)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Text(
                            "अनिवार्य 5 श्रेणियों की फोटो और 'अन्य' में असीमित (No Limit) फोटो व वीडियो अपलोड करें।",
                            fontSize = 12.sp,
                            color = Slate500
                        )

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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(category.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                            Text(
                                                text = if (category.minRequired > 0) "Mandatory (Min ${category.minRequired} required)" else "Optional (Unlimited Uploads - No Limit)",
                                                fontSize = 11.sp,
                                                color = if (isSatisfied) Emerald600 else Red600
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (category.supportsVideo) {
                                                // Video picker button
                                                IconButton(
                                                    onClick = {
                                                        activePhotoCategory = category
                                                        videoPickerLauncher.launch("video/*")
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Videocam, contentDescription = "Add Video", tint = Indigo600)
                                                }
                                            }

                                            // Photo picker button
                                            IconButton(
                                                onClick = {
                                                    activePhotoCategory = category
                                                    photoPickerLauncher.launch("image/*")
                                                }
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = "Add Photo", tint = Indigo600)
                                            }
                                        }
                                    }

                                    if (category.supportsVideo) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    activePhotoCategory = category
                                                    photoPickerLauncher.launch("image/*")
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp), tint = Indigo600)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("+ Add Photos", fontSize = 12.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    activePhotoCategory = category
                                                    videoPickerLauncher.launch("video/*")
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = Indigo600)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("+ Add Videos", fontSize = 12.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (currentList.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(84.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .clickable {
                                                    activePhotoCategory = category
                                                    if (category.supportsVideo) {
                                                        photoPickerLauncher.launch("image/*")
                                                    } else {
                                                        photoPickerLauncher.launch("image/*")
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Add, contentDescription = "Attached Photo", tint = Slate500)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (category.supportsVideo) "Tap to upload photos or videos (No limit)" else "Tap to capture / upload photo",
                                                    fontSize = 13.sp,
                                                    color = Slate500
                                                )
                                            }
                                        }
                                    } else {
                                        Column {
                                            Text(
                                                text = "${currentList.size} item(s) attached" + if (category.supportsVideo) " (Unlimited)" else "",
                                                fontSize = 11.sp,
                                                color = Slate500,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                items(currentList) { uriStr ->
                                                    val isVideo = MediaStorageHelper.isMediaVideo(uriStr, context)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(88.dp)
                                                            .border(1.5.dp, if (isVideo) Indigo600 else Emerald600, RoundedCornerShape(12.dp))
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                if (isVideo) {
                                                                    MediaStorageHelper.openMedia(context, uriStr)
                                                                } else {
                                                                    previewMediaUrl = uriStr
                                                                }
                                                            }
                                                    ) {
                                                        AsyncImage(
                                                            model = uriStr,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )

                                                        // If Video, show video badge and play overlay
                                                        if (isVideo) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(Color.Black.copy(alpha = 0.35f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Icon(
                                                                        Icons.Default.PlayCircleFilled,
                                                                        contentDescription = "Play Video",
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(32.dp)
                                                                    )
                                                                    Text("VIDEO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                                }
                                                            }
                                                        }

                                                        // Delete Badge Button
                                                        IconButton(
                                                            onClick = {
                                                                val updatedList = (photoMap[category.categoryId] ?: mutableListOf()).toMutableList()
                                                                updatedList.remove(uriStr)
                                                                photoMap[category.categoryId] = updatedList
                                                            },
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .align(Alignment.TopEnd)
                                                                .padding(2.dp)
                                                                .clip(CircleShape)
                                                                .background(Red600.copy(alpha = 0.85f))
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Remove",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
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

                                val combinedBciDetails = if (bciName.isNotBlank() || bciMobile.isNotBlank()) {
                                    listOf(bciName.trim(), bciMobile.trim()).filter { it.isNotBlank() }.joinToString(" - ")
                                } else {
                                    ""
                                }

                                val answers = VisitAnswers(
                                    q1_soeName = employeeUser.name,
                                    q2_visitDate = visitDate,
                                    q3_schoolName = schoolName,
                                    q4_udiseCode = udiseCode,
                                    q5_district = district,
                                    q6_block = block,
                                    q7_principalName = principalName,
                                    q8_principalMobile = principalMobile,
                                    q9_metPrincipal = metPrincipal,
                                    q10_missionGyanAwareness = missionGyanAwareness,
                                    q11_studentCount = studentCount,
                                    q12_schoolResponse = schoolResponse,
                                    q13_bciName = bciName.trim(),
                                    q13_bciMobile = bciMobile.trim(),
                                    q13_bciContactDetails = combinedBciDetails,
                                    q14_whatsappGroupAdded = whatsappGroupAdded,
                                    q15_posterInstalled = posterInstalled,
                                    q16_keyObservations = keyObservations,
                                    q17_problemsOrAssistance = problemsOrAssistance,
                                    q18_followupRequired = followupRequired,
                                    q20_finalRemarks = finalRemarks,
                                    q21_smartClassStatus = smartClassStatus,
                                    q22_participatingClasses = selectedClasses.sorted().joinToString(", "),
                                    q23_state = stateName
                                )

                                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                                val answersAdapter = moshi.adapter(VisitAnswers::class.java)
                                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
                                val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

                                val visitId = existingVisit?.visitId ?: task?.visitId ?: ("vst_" + UUID.randomUUID().toString().take(8))
                                val schoolId = existingVisit?.schoolId ?: task?.schoolId ?: initialSchool?.schoolId ?: ("sch_" + UUID.randomUUID().toString().take(8))

                                val finalVisit = Visit(
                                    visitId = visitId,
                                    schoolId = schoolId,
                                    employeeId = employeeUser.userId,
                                    employeeName = employeeUser.name,
                                    schoolName = schoolName,
                                    state = stateName,
                                    district = district,
                                    block = block,
                                    visitDate = visitDate,
                                    status = VisitStatus.SUBMITTED,
                                    answersJson = answersAdapter.toJson(answers),
                                    photosJson = photosAdapter.toJson(photoMap.mapValues { it.value.toList() }),
                                    createdAt = existingVisit?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )

                                onSubmitVisit(finalVisit) { res ->
                                    isSubmitting = false
                                    if (res.isSuccess) {
                                        if (isOnline) {
                                            submissionSuccessMessage = "विज़िट रिपोर्ट सफलतापूर्वक सबमिट और सर्वर पर सिंक हो गई है।"
                                        } else {
                                            submissionSuccessMessage = "विज़िट रिपोर्ट स्थानीय रूप से सुरक्षित सहेज ली गई है (Saved Locally)!\n\nनेटवर्क उपलब्ध होते ही ऐप इसे अपने आप सर्वर पर अपलोड (Sync) कर देगा।"
                                        }
                                        showSubmissionSuccessDialog = true
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

    // Submission Success Dialog (Offline / Online)
    if (showSubmissionSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSubmissionSuccessDialog = false
                onBackClick()
            },
            icon = {
                Icon(
                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Emerald600,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = if (isOnline) "Report Submitted & Synced" else "Report Saved Locally (ऑफलाइन सुरक्षित)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = submissionSuccessMessage,
                    fontSize = 14.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmissionSuccessDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK / डैशबोर्ड पर जाएँ", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Full Media Preview Dialog
    if (previewMediaUrl != null) {
        Dialog(onDismissRequest = { previewMediaUrl = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val isVid = MediaStorageHelper.isMediaVideo(previewMediaUrl ?: "", context)
                    if (isVid) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    MediaStorageHelper.openMedia(context, previewMediaUrl ?: "")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PlayCircleFilled,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to Play Video (वीडियो चलाएँ)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        AsyncImage(
                            model = previewMediaUrl,
                            contentDescription = "Full Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    IconButton(
                        onClick = { previewMediaUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
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
                        value = udiseCode,
                        onValueChange = { udiseCode = it },
                        label = { Text("UDISE Code") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stateName,
                        onValueChange = { stateName = it },
                        label = { Text("State") },
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
                                    stateName = stateName,
                                    districtName = district,
                                    blockName = block,
                                    principalName = principalName,
                                    principalMobile = principalMobile
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
