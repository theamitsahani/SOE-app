package com.example.ui.admin

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PhotoCategory
import com.example.data.model.Visit
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.util.ExcelHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class PhotoGridItem(
    val url: String,
    val categoryName: String,
    val schoolName: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryTab(
    visits: List<Visit>
) {
    var selectedSchoolName by remember { mutableStateOf("All Schools") }
    var selectedCategory by remember { mutableStateOf("All Categories") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
    val photosAdapter = moshi.adapter<Map<String, List<String>>>(mapType)

    val allPhotos = remember(visits) {
        val list = mutableListOf<PhotoGridItem>()
        for (v in visits) {
            try {
                val photoMap = photosAdapter.fromJson(v.photosJson) ?: emptyMap()
                for ((catId, urls) in photoMap) {
                    val catObj = PhotoCategory.fromId(catId)
                    for (u in urls) {
                        list.add(
                            PhotoGridItem(
                                url = u,
                                categoryName = catObj.displayName,
                                schoolName = v.schoolName,
                                date = v.visitDate
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        list
    }

    val schoolNamesList = remember(allPhotos) {
        listOf("All Schools") + allPhotos.map { it.schoolName }.distinct()
    }

    val filteredPhotos = remember(allPhotos, selectedSchoolName, selectedCategory) {
        allPhotos.filter {
            (selectedSchoolName == "All Schools" || it.schoolName == selectedSchoolName) &&
                    (selectedCategory == "All Categories" || it.categoryName == selectedCategory)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("School Photo Gallery", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy900)
                Text("${filteredPhotos.size} photos available", fontSize = 12.sp, color = Slate500)
            }

            if (selectedSchoolName != "All Schools") {
                Button(
                    onClick = {
                        ExcelHelper.downloadSchoolPhotosZip(context, selectedSchoolName, filteredPhotos.map { it.url })
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download ZIP", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // School Filter Dropdown
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedSchoolName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter by School") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                schoolNamesList.forEach { sName ->
                    DropdownMenuItem(
                        text = { Text(sName, fontSize = 13.sp) },
                        onClick = {
                            selectedSchoolName = sName
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredPhotos.isEmpty()) {
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
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No photos found for selected filters", fontSize = 14.sp, color = Slate500)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredPhotos) { photo ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                AsyncImage(
                                    model = photo.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(photo.categoryName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Text(photo.schoolName, fontSize = 11.sp, color = Slate500, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
