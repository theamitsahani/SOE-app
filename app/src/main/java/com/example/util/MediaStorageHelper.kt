package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object MediaStorageHelper {

    /**
     * Copies a picked Uri (content://) to the app's persistent internal storage
     * so that media remains available indefinitely, even offline, across app restarts.
     */
    suspend fun saveMediaLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(sourceUri)
            val extension = if (mimeType != null) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            } else {
                val uriStr = sourceUri.toString().lowercase()
                when {
                    uriStr.contains(".mp4") -> "mp4"
                    uriStr.contains(".mov") -> "mov"
                    uriStr.contains(".3gp") -> "3gp"
                    uriStr.contains(".mkv") -> "mkv"
                    uriStr.contains(".png") -> "png"
                    uriStr.contains(".webp") -> "webp"
                    else -> "jpg"
                }
            }

            val mediaDir = File(context.filesDir, "visit_media").apply {
                if (!exists()) mkdirs()
            }

            val prefix = if (isMimeOrUriVideo(mimeType, sourceUri.toString())) "vid_" else "img_"
            val destFile = File(mediaDir, "${prefix}${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension")

            contentResolver.openInputStream(sourceUri)?.use { input: InputStream ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to original URI string if file copy fails
            sourceUri.toString()
        }
    }

    /**
     * Checks if a given media path or URL represents a video.
     */
    fun isMediaVideo(pathOrUrl: String, context: Context? = null): Boolean {
        val lower = pathOrUrl.lowercase()
        if (lower.contains("vid_") ||
            lower.contains(".mp4") ||
            lower.contains(".mov") ||
            lower.contains(".3gp") ||
            lower.contains(".mkv") ||
            lower.contains(".webm") ||
            lower.contains(".avi")
        ) {
            return true
        }

        if (context != null && (pathOrUrl.startsWith("content://") || pathOrUrl.startsWith("file://"))) {
            try {
                val mime = context.contentResolver.getType(Uri.parse(pathOrUrl))
                if (mime?.startsWith("video/") == true) {
                    return true
                }
            } catch (_: Exception) {}
        }

        return false
    }

    private fun isMimeOrUriVideo(mimeType: String?, uriString: String): Boolean {
        if (mimeType?.startsWith("video/") == true) return true
        val lower = uriString.lowercase()
        return lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".3gp") || lower.contains(".mkv") || lower.contains(".webm")
    }

    /**
     * Generates structured Google Drive folder path:
     * SOE VISIT / State / District / Block / School Name / Visit Date /
     */
    fun getDriveFolderPath(
        state: String,
        district: String,
        block: String,
        schoolName: String,
        visitDate: String
    ): String {
        fun sanitize(s: String) = s.replace("/", "-").replace("\\", "-").trim().ifBlank { "Unknown" }
        val sState = sanitize(state.ifBlank { "Rajasthan" })
        val sDistrict = sanitize(district)
        val sBlock = sanitize(block)
        val sSchool = sanitize(schoolName)
        val sDate = sanitize(visitDate)

        return "SOE VISIT/$sState/$sDistrict/$sBlock/$sSchool/$sDate"
    }

    /**
     * Generates standardized filename for each photo category:
     * 1. School Photo -> School_Photo.jpg
     * 2. Explaining Our App -> Explaining_App.jpg
     * 3. Students Using Smart Board -> Smart_Board.jpg
     * 4. Photo With Principal Sir -> Principal.jpg
     * 5. Letter Photo -> Letter.jpg
     * 6. Other Photos -> Other_01.jpg, Other_02.jpg...
     */
    fun getStandardizedFileName(categoryId: String, index: Int, extension: String = "jpg"): String {
        val ext = extension.trimStart('.')
        return when (categoryId.lowercase()) {
            "school_photo" -> "School_Photo.$ext"
            "explaining_app" -> "Explaining_App.$ext"
            "students_smart_board" -> "Smart_Board.$ext"
            "principal_photo" -> "Principal.$ext"
            "letter_photo" -> "Letter.$ext"
            else -> "Other_${String.format("%02d", index + 1)}.$ext"
        }
    }

    /**
     * Launches a viewer or system player for photo/video.
     */
    fun openMedia(context: Context, pathOrUrl: String) {
        try {
            val isVideo = isMediaVideo(pathOrUrl, context)
            val uri = when {
                pathOrUrl.startsWith("file://") -> {
                    val file = File(Uri.parse(pathOrUrl).path ?: "")
                    if (file.exists()) {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    } else {
                        Uri.parse(pathOrUrl)
                    }
                }
                pathOrUrl.startsWith("/") -> {
                    val file = File(pathOrUrl)
                    if (file.exists()) {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    } else {
                        Uri.parse(pathOrUrl)
                    }
                }
                else -> Uri.parse(pathOrUrl)
            }

            val mime = if (isVideo) "video/*" else "image/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
