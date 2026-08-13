package com.example.util

import android.content.Context
import com.example.data.model.PhotoCategory
import com.example.data.model.Visit
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipHelper {

    /**
     * Packages selected visits/schools into a single ZIP file organized school-wise.
     */
    fun createSchoolWisePhotosZip(context: Context, visits: List<Visit>): File {
        val zipFile = File(context.cacheDir, "School_Photos_${System.currentTimeMillis()}.zip")
        val zos = ZipOutputStream(FileOutputStream(zipFile))

        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
        val adapter = moshi.adapter<Map<String, List<String>>>(type)

        for (v in visits) {
            val sanitizeSchoolName = v.schoolName.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "School_${v.schoolId}" }
            val photoMap: Map<String, List<String>> = try {
                if (v.photosJson.isNotBlank()) adapter.fromJson(v.photosJson) ?: emptyMap() else emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            PhotoCategory.entries.forEach { category ->
                val categoryDirName = category.displayName.replace(Regex("""[\\/:*?"<>|]"""), "_")
                val urls = photoMap[category.categoryId] ?: emptyList()

                urls.forEachIndexed { index, url ->
                    val fileName = "photo_${index + 1}.jpg"
                    val entryPath = "$sanitizeSchoolName/$categoryDirName/$fileName"

                    zos.putNextEntry(ZipEntry(entryPath))

                    // If it's a local file path, write file bytes. Otherwise generate placeholder bytes
                    if (url.startsWith("/") || url.startsWith("file://")) {
                        val localPath = url.removePrefix("file://")
                        val file = File(localPath)
                        if (file.exists()) {
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                        } else {
                            zos.write(generateDummyPhotoBytes(category.displayName))
                        }
                    } else {
                        zos.write(generateDummyPhotoBytes(category.displayName))
                    }
                    zos.closeEntry()
                }
            }
        }

        zos.flush()
        zos.close()
        return zipFile
    }

    private fun generateDummyPhotoBytes(text: String): ByteArray {
        // Simple byte placeholder or image bytes representation
        return "JPEG DATA PLACEHOLDER FOR $text".toByteArray(Charsets.UTF_8)
    }
}
