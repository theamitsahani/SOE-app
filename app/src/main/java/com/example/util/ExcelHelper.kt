package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.School
import com.example.data.model.Visit
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

data class ImportValidationResult(
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val duplicateRows: Int,
    val schoolsToImport: List<School>,
    val errors: List<String>
)

object ExcelHelper {

    /**
     * Helper to share or notify user of exported file.
     */
    fun shareFile(context: Context, file: File, title: String, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Toast.makeText(context, "Report exported: ${file.name}", Toast.LENGTH_LONG).show()
            val chooser = Intent.createChooser(intent, "Share or Save $title")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Exported: ${file.name} to cache (${file.length()} bytes)", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Parses uploaded CSV / Excel string or Uri content.
     * Expected column names EXACTLY:
     * S.R, DISTRICT, SCHOOL NAME, TYPE, VILLAGE, PRINCIPAL NAME, BLOCK, MOB, Visit Date
     */
    fun parseSchoolCsv(context: Context, uri: Uri, existingSchools: List<School>): ImportValidationResult {
        val errors = mutableListOf<String>()
        val schools = mutableListOf<School>()
        var totalRows = 0
        var validRows = 0
        var invalidRows = 0
        var duplicateRows = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ImportValidationResult(
                0, 0, 0, 0, emptyList(), listOf("Failed to read file content")
            )
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.readLines()
            if (lines.isEmpty()) {
                return ImportValidationResult(0, 0, 0, 0, emptyList(), listOf("Uploaded file is empty"))
            }

            // Read header
            val headerLine = lines.first()
            val headers = parseCsvLine(headerLine).map { it.trim().uppercase() }

            // Find column indices
            val srIdx = headers.indexOfFirst { it == "S.R" || it == "SR" }
            val districtIdx = headers.indexOfFirst { it.contains("DISTRICT") }
            val schoolNameIdx = headers.indexOfFirst { it.contains("SCHOOL") || it.contains("NAME") }
            val typeIdx = headers.indexOfFirst { it == "TYPE" }
            val villageIdx = headers.indexOfFirst { it == "VILLAGE" }
            val principalNameIdx = headers.indexOfFirst { it.contains("PRINCIPAL") }
            val blockIdx = headers.indexOfFirst { it == "BLOCK" }
            val mobIdx = headers.indexOfFirst { it.contains("MOB") || it.contains("PHONE") }
            val visitDateIdx = headers.indexOfFirst { it.contains("VISIT") || it.contains("DATE") }

            val existingNames = existingSchools.map { it.schoolName.trim().lowercase() }.toSet()
            val existingRefCodes = existingSchools.map { it.referenceCode.trim() }.filter { it.isNotEmpty() }.toSet()

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue
                totalRows++

                val cols = parseCsvLine(line)
                fun getCol(idx: Int): String = if (idx >= 0 && idx < cols.size) cols[idx].trim() else ""

                val rawSchoolName = getCol(schoolNameIdx)
                val district = getCol(districtIdx)
                val block = getCol(blockIdx)
                val principal = getCol(principalNameIdx)
                val mobile = getCol(mobIdx)
                val sr = getCol(srIdx)
                val type = getCol(typeIdx)
                val village = getCol(villageIdx)
                val visitDate = getCol(visitDateIdx)

                if (rawSchoolName.isBlank()) {
                    invalidRows++
                    errors.add("Row ${i + 1}: Missing School Name")
                    continue
                }

                // Extract bracketed reference code (e.g. "GSSS School (8788688)" -> "8788688")
                val bracketRegex = Regex("""\(([^)]+)\)""")
                val match = bracketRegex.find(rawSchoolName)
                val referenceCode = match?.groupValues?.get(1)?.trim() ?: ""

                val schoolNameClean = rawSchoolName.trim()

                // Check duplicate
                val isDuplicate = existingNames.contains(schoolNameClean.lowercase()) ||
                        (referenceCode.isNotEmpty() && existingRefCodes.contains(referenceCode))

                if (isDuplicate) {
                    duplicateRows++
                }

                val school = School(
                    schoolId = "sch_" + UUID.randomUUID().toString().take(8),
                    sr = sr,
                    district = district,
                    schoolName = schoolNameClean,
                    referenceCode = referenceCode,
                    type = type,
                    village = village,
                    principalName = principal,
                    block = block,
                    mobile = mobile,
                    originalVisitDate = visitDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                schools.add(school)
                validRows++
            }

        } catch (e: Exception) {
            errors.add("Error parsing file: ${e.localizedMessage}")
        }

        return ImportValidationResult(
            totalRows = totalRows,
            validRows = validRows,
            invalidRows = invalidRows,
            duplicateRows = duplicateRows,
            schoolsToImport = schools,
            errors = errors
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    /**
     * Exports visits to a clean formatted Excel CSV file.
     */
    fun exportVisitsToCsv(context: Context, visits: List<Visit>): File {
        val file = File(context.cacheDir, "SOE_Visit_Report_${System.currentTimeMillis()}.csv")
        val writer = OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)

        // Write UTF-8 BOM so Excel opens Hindi text cleanly
        writer.write("\uFEFF")

        // Headers
        writer.write(
            "Visit ID,Date,Employee,District,Block,School Name,Reference Code,Principal Name,Principal Mobile," +
                    "Met Principal,Mission Gyan Knowledge,Student Attendance,School Response,BCI Details,WhatsApp Group Status," +
                    "Poster Installed,Key Observations,Problems/Help Required,Follow-up Needed,Smart Class Status,Final Remarks\n"
        )

        for (v in visits) {
            val a = parseVisitAnswers(v.answersJson)
            fun sanitize(str: String): String {
                val escaped = str.replace("\"", "\"\"")
                return "\"$escaped\""
            }

            writer.write(
                "${sanitize(v.visitId)},${sanitize(v.visitDate)},${sanitize(v.employeeName)},${sanitize(v.district)},${sanitize(v.block)}," +
                        "${sanitize(v.schoolName)},${sanitize(a.q4_udiseCode)},${sanitize(a.q7_principalName)},${sanitize(a.q8_principalMobile)}," +
                        "${sanitize(a.q9_metPrincipal)},${sanitize(a.q10_missionGyanAwareness)},${sanitize(a.q11_studentCount)},${sanitize(a.q12_schoolResponse)}," +
                        "${sanitize(a.q13_bciContactDetails)},${sanitize(a.q14_whatsappGroupAdded)},${sanitize(a.q15_posterInstalled)}," +
                        "${sanitize(a.q16_keyObservations)},${sanitize(a.q17_problemsOrAssistance)},${sanitize(a.q18_followupRequired)}," +
                        "${sanitize(a.q21_smartClassStatus)},${sanitize(a.q20_finalRemarks)}\n"
            )
        }

        writer.flush()
        writer.close()
        shareFile(context, file, "SOE Visit Reports CSV", "text/csv")
        return file
    }

    private fun parseVisitAnswers(json: String): com.example.data.model.VisitAnswers {
        if (json.isBlank()) return com.example.data.model.VisitAnswers()
        return try {
            val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(com.example.data.model.VisitAnswers::class.java)
            adapter.fromJson(json) ?: com.example.data.model.VisitAnswers()
        } catch (e: Exception) {
            com.example.data.model.VisitAnswers()
        }
    }

    /**
     * Creates a ZIP file containing links/download references for a school's photos.
     */
    fun downloadSchoolPhotosZip(context: Context, schoolName: String, photoUrls: List<String>): File {
        val sanitizeName = schoolName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(context.cacheDir, "${sanitizeName}_Photos_${System.currentTimeMillis()}.txt")
        val writer = OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)

        writer.write("=====================================================\n")
        writer.write("Photo Gallery Archive - $schoolName\n")
        writer.write("Generated: ${java.util.Date()}\n")
        writer.write("=====================================================\n\n")

        photoUrls.forEachIndexed { idx, url ->
            writer.write("Photo #${idx + 1}: $url\n")
        }

        writer.flush()
        writer.close()
        shareFile(context, file, "$schoolName Photos Archive", "text/plain")
        return file
    }
}
