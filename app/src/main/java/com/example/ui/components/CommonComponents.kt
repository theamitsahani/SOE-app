package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserStatus
import com.example.data.model.VisitStatus
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun SyncStatusBanner(
    isOnline: Boolean,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (!isOnline) Red100 else if (pendingCount > 0) Amber100 else Emerald100
    val contentColor = if (!isOnline) Red600 else if (pendingCount > 0) Amber600 else Emerald600
    val icon = if (!isOnline) Icons.Default.CloudOff else if (pendingCount > 0) Icons.Default.CloudQueue else Icons.Default.CloudDone
    val text = if (!isOnline) "Offline Mode • Data saved locally"
    else if (pendingCount > 0) "$pendingCount Visit(s) Pending Sync"
    else "Online • All Data Synced"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }

            if (pendingCount > 0 && isOnline) {
                OutlinedButton(
                    onClick = onSyncClick,
                    modifier = Modifier.padding(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
                ) {
                    Text("Sync Now", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatusChip(statusName: String) {
    val (bgColor, textColor) = when (statusName.uppercase()) {
        "SUBMITTED", "COMPLETED", "ACTIVE" -> Pair(Emerald100, Emerald600)
        "ASSIGNED", "STARTED", "IN_PROGRESS", "INACTIVE" -> Pair(Amber100, Amber600)
        "REVIEWED" -> Pair(Color(0xFFE0E7FF), Indigo600)
        else -> Pair(Slate200, Slate700)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = statusName.replace("_", " "),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Slate500) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Slate500)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Slate900,
            unfocusedTextColor = Slate900,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Indigo600,
            unfocusedBorderColor = Slate300
        ),
        modifier = modifier.fillMaxWidth()
    )
}
