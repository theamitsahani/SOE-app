package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.PhotoCategory
import com.example.data.model.School
import com.example.data.model.Visit
import com.example.data.model.VisitAnswers
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.MediaStorageHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Composable
fun VisitDetailDialog(
    visit: Visit,
    school: School? = null,
    onDismiss: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    isEditable: Boolean = false,
    editTimeRemainingText: String = ""
) {
    val context = LocalContext.current
    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }

    val answers = remember(visit.answersJson) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            moshi.adapter(VisitAnswers::class.java).fromJson(visit.answersJson) ?: VisitAnswers()
        } catch (e: Exception) {
            VisitAnswers()
        }
    }

    val photoMap = remember(visit.photosJson) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
            val adapter = moshi.adapter<Map<String, List<String>>>(mapType)
            adapter.fromJson(visit.photosJson) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Consolidated Principal details
    val principalName = answers.q7_principalName.ifBlank { school?.principalName?.ifBlank { "Not Specified" } ?: "Not Specified" }
    val principalMobile = answers.q8_principalMobile.ifBlank { school?.principalMobile?.ifBlank { school?.mobile ?: "" } ?: "" }

    // Consolidated BCI details
    val bciName = answers.q13_bciName.ifBlank {
        if (answers.q13_bciContactDetails.contains("-")) answers.q13_bciContactDetails.substringBefore("-").trim()
        else answers.q13_bciContactDetails
    }
    val bciMobile = answers.q13_bciMobile.ifBlank {
        if (answers.q13_bciContactDetails.contains("-")) answers.q13_bciContactDetails.substringAfter("-").trim()
        else ""
    }

    // Consolidated Village & UDISE
    val villageName = school?.villageName?.ifBlank { "Not Specified" } ?: "Not Specified"
    val udiseCode = answers.q4_udiseCode.ifBlank { school?.referenceCode ?: "Not Available" }
    val schoolType = school?.schoolType?.ifBlank { "Government School" } ?: "Government School"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.94f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = Navy900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Indigo600),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = visit.schoolName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Full Visit Report (सम्पूर्ण विज़िट विवरण)",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Scrollable Content Body - All Details Aligned on a Single Page
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF8FAFC))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status & Officer Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(statusName = visit.status.name)
                                Text(
                                    text = "Date: ${visit.visitDate}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Indigo600, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Field Officer: ${visit.employeeName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Navy900
                                    )
                                }

                                if (isEditable && editTimeRemainingText.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = editTimeRemainingText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Indigo600,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1. School Master Details (स्कूल की मास्टर जानकारी)
                    DetailSectionCard(
                        title = "1. School & Master Information (स्कूल मास्टर विवरण)",
                        icon = Icons.Default.School
                    ) {
                        DetailGridRow("School Name (विद्यालय का नाम)", visit.schoolName)
                        DetailGridRow("School Type / Category", schoolType)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                DetailGridRow("District (जिला)", visit.district)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DetailGridRow("Block (ब्लॉक)", visit.block)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                DetailGridRow("Village (गांव का नाम)", villageName)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DetailGridRow("State (राज्य)", visit.state.ifBlank { "Rajasthan" })
                            }
                        }
                        DetailGridRow("UDISE / Ref Code (यू-डाइस कोड)", udiseCode)
                    }

                    // 2. Principal Contact Details (प्रधानाचार्य संपर्क विवरण) with Direct Call
                    DetailSectionCard(
                        title = "2. Principal Information (प्रधानाचार्य विवरण)",
                        icon = Icons.Default.Person
                    ) {
                        DetailGridRow("Principal Name (प्रधानाचार्य का नाम)", principalName)

                        // Call Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Principal Mobile (मोबाइल नंबर)", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (principalMobile.isNotBlank()) principalMobile else "Not Provided",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                }

                                if (principalMobile.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$principalMobile"))
                                            context.startActivity(dialIntent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. BCI Contact Details (BCI अधिकारी विवरण - Point 13) with Direct Call
                    DetailSectionCard(
                        title = "3. BCI Officer Details (BCI अधिकारी विवरण)",
                        icon = Icons.Default.Phone
                    ) {
                        DetailGridRow("BCI Officer Name (BCI का नाम)", bciName.ifBlank { "Not Specified" })

                        // BCI Call Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("BCI Contact Number (BCI मोबाइल नंबर)", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (bciMobile.isNotBlank()) bciMobile else "Not Provided",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                }

                                if (bciMobile.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$bciMobile"))
                                            context.startActivity(dialIntent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call BCI", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call BCI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Form Questionnaire Responses (विज़िट फॉर्म के सभी बिंदु)
                    DetailSectionCard(
                        title = "4. Field Survey Responses (विज़िट फॉर्म प्रश्नोत्तरी)",
                        icon = Icons.Default.Assignment
                    ) {
                        DetailGridRow("9. Met Principal? (प्रधानाचार्य से मुलाकात)", answers.q9_metPrincipal.ifBlank { "हाँ" })
                        DetailGridRow("10. Mission Gyan App Awareness (ऐप की जानकारी)", answers.q10_missionGyanAwareness.ifBlank { "हाँ" })
                        DetailGridRow("11. Participating Classes (शामिल कक्षाएं)", answers.q22_participatingClasses.ifBlank { "Class 6th to 12th" })
                        DetailGridRow("12. Student Attendance Count (उपस्थित विद्यार्थी संख्या)", answers.q11_studentCount.ifBlank { "Not Recorded" })
                        DetailGridRow("13. School Response / Reception (विद्यालय प्रतिक्रिया)", answers.q12_schoolResponse.ifBlank { "बहुत अच्छी" })
                        DetailGridRow("14. Added to WhatsApp Group (WhatsApp ग्रुप में जोड़े गए)", answers.q14_whatsappGroupAdded.ifBlank { "हाँ" })
                        DetailGridRow("15. Poster Installed Status (पोस्टर लगाया गया)", answers.q15_posterInstalled.ifBlank { "हाँ" })
                        DetailGridRow("21. Smart Class Status (स्मार्ट क्लास की स्थिति)", answers.q21_smartClassStatus.ifBlank { "बहुत अच्छी" })
                        DetailGridRow("16. Key Observations (मुख्य अवलोकन)", answers.q16_keyObservations.ifBlank { "None" })
                        DetailGridRow("17. Problems / Help Required (समस्याएं / आवश्यकता)", answers.q17_problemsOrAssistance.ifBlank { "None" })
                        DetailGridRow("18. Follow-up Required (फॉलो-अप आवश्यकता)", answers.q18_followupRequired.ifBlank { "नहीं" })
                        DetailGridRow("20. Final Remarks (अंतिम टिप्पणी)", answers.q20_finalRemarks.ifBlank { "None" })
                    }

                    // 5. Uploaded Photos Section
                    val totalPhotos = photoMap.values.sumOf { it.size }
                    DetailSectionCard(
                        title = "5. Attached Photos ($totalPhotos Photographs)",
                        icon = Icons.Default.Image
                    ) {
                        if (totalPhotos == 0) {
                            Text(
                                text = "No photos uploaded for this visit.",
                                fontSize = 12.sp,
                                color = Slate500,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PhotoCategory.entries.forEach { category ->
                                    val urls = photoMap[category.categoryId] ?: emptyList()
                                    if (urls.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "${category.displayName} (${urls.size})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(urls) { url ->
                                                    val isVideo = MediaStorageHelper.isMediaVideo(url, context)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(80.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .border(1.dp, if (isVideo) Indigo600 else Slate200, RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                if (isVideo) {
                                                                    MediaStorageHelper.openMedia(context, url)
                                                                } else {
                                                                    previewPhotoUrl = url
                                                                }
                                                            }
                                                    ) {
                                                        AsyncImage(
                                                            model = url,
                                                            contentDescription = category.displayName,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )

                                                        if (isVideo) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(Color.Black.copy(alpha = 0.35f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.PlayCircleFilled,
                                                                    contentDescription = "Play Video",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(28.dp)
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

                // Bottom Action Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close (बंद करें)", fontWeight = FontWeight.SemiBold)
                        }

                        if (isEditable && onEditClick != null) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onEditClick()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Visit (संशोधन करें)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Photo Preview Dialog
    if (previewPhotoUrl != null) {
        Dialog(onDismissRequest = { previewPhotoUrl = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = previewPhotoUrl,
                        contentDescription = "Full Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { previewPhotoUrl = null },
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
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
            }
            Divider(color = Slate100, thickness = 1.dp)
            content()
        }
    }
}

@Composable
private fun DetailGridRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value.ifBlank { "Not Specified" },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700
        )
    }
}
