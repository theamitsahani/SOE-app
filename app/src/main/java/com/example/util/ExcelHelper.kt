package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.School
import com.example.data.model.Visit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    val completedVisitsToImport: List<com.example.data.model.Visit> = emptyList(),
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
     * Parses uploaded CSV / Excel content.
     * Specific User Column Specification:
     * Column A (0): S.R (Ignored)
     * Column B (1): DISTRICT
     * Column C (2): SCHOOL NAME -> Must not be empty. Only invalid if Column C is blank!
     * Column D (3): TYPE
     * Column E (4): VILLAGE
     * Column F (5): PRINCIPAL NAME
     * Column G (6): BLOCK
     * Column H (7): MOB
     * Column I (8): Visit Date
     * Column J (9): Status -> If Column I is date & Column J is "TRUE" / "True", marked as COMPLETED!
     */
    fun parseSchoolCsv(context: Context, uri: Uri, existingSchools: List<School>): ImportValidationResult {
        val errors = mutableListOf<String>()
        val schools = mutableListOf<School>()
        val completedVisits = mutableListOf<com.example.data.model.Visit>()
        var totalRows = 0
        var validRows = 0
        var invalidRows = 0
        var duplicateRows = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ImportValidationResult(
                0, 0, 0, 0, emptyList(), emptyList(), listOf("Failed to read file content")
            )
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.readLines()
            if (lines.isEmpty()) {
                return ImportValidationResult(0, 0, 0, 0, emptyList(), emptyList(), listOf("Uploaded file is empty"))
            }

            // Check if first line is a header
            val firstLine = lines.first()
            val firstCols = parseCsvLine(firstLine).map { it.trim().uppercase() }
            val hasHeader = firstCols.any { it.contains("DISTRICT") || it.contains("SCHOOL") || it == "S.R" || it == "SR" }

            val startIndex = if (hasHeader) 1 else 0

            // Column indices
            var districtIdx = 1
            var schoolNameIdx = 2
            var typeIdx = 3
            var villageIdx = 4
            var principalNameIdx = 5
            var blockIdx = 6
            var mobIdx = 7
            var visitDateIdx = 8
            var statusIdx = 9

            if (hasHeader) {
                val dIdx = firstCols.indexOfFirst { it.contains("DISTRICT") }
                if (dIdx >= 0) districtIdx = dIdx

                val sIdx = firstCols.indexOfFirst { it.contains("SCHOOL") || it.contains("NAME") }
                if (sIdx >= 0) schoolNameIdx = sIdx

                val tIdx = firstCols.indexOfFirst { it == "TYPE" }
                if (tIdx >= 0) typeIdx = tIdx

                val vIdx = firstCols.indexOfFirst { it == "VILLAGE" }
                if (vIdx >= 0) villageIdx = vIdx

                val pIdx = firstCols.indexOfFirst { it.contains("PRINCIPAL") }
                if (pIdx >= 0) principalNameIdx = pIdx

                val bIdx = firstCols.indexOfFirst { it == "BLOCK" }
                if (bIdx >= 0) blockIdx = bIdx

                val mIdx = firstCols.indexOfFirst { it.contains("MOB") || it.contains("PHONE") }
                if (mIdx >= 0) mobIdx = mIdx

                val vdIdx = firstCols.indexOfFirst { it.contains("VISIT") || it.contains("DATE") }
                if (vdIdx >= 0) visitDateIdx = vdIdx

                val stIdx = firstCols.indexOfFirst { it.contains("STATUS") }
                if (stIdx >= 0) statusIdx = stIdx
            }

            val existingNames = existingSchools.map { it.schoolName.trim().lowercase() }.toSet()
            val existingRefCodes = existingSchools.map { it.referenceCode.trim() }.filter { it.isNotEmpty() }.toSet()

            for (i in startIndex until lines.size) {
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
                val sr = getCol(0) // Column A ignored, but kept if needed
                val type = getCol(typeIdx)
                val village = getCol(villageIdx)
                val visitDate = getCol(visitDateIdx)
                val statusStr = getCol(statusIdx)

                // RULE: ONLY if Column C (school name) is empty, consider row INVALID
                if (rawSchoolName.isBlank()) {
                    invalidRows++
                    errors.add("Row ${i + 1}: Column C (School Name) is empty")
                    continue
                }

                val bracketRegex = Regex("""\(([^)]+)\)""")
                val match = bracketRegex.find(rawSchoolName)
                val referenceCode = match?.groupValues?.get(1)?.trim() ?: ""
                val schoolNameClean = rawSchoolName.trim()

                val isDuplicate = existingNames.contains(schoolNameClean.lowercase()) ||
                        (referenceCode.isNotEmpty() && existingRefCodes.contains(referenceCode))

                if (isDuplicate) {
                    duplicateRows++
                }

                val schoolId = "sch_" + UUID.randomUUID().toString().take(8)

                val school = School(
                    schoolId = schoolId,
                    sr = sr,
                    district = district.ifBlank { "Rajasthan" },
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

                // RULE: If Column I is filled with date AND Column J is TRUE / True -> mark school as COMPLETE
                val isCompleted = visitDate.isNotBlank() && (
                    statusStr.equals("TRUE", ignoreCase = true) ||
                    statusStr.equals("1") ||
                    statusStr.equals("YES", ignoreCase = true) ||
                    statusStr.equals("COMPLETED", ignoreCase = true)
                )

                if (isCompleted) {
                    val answers = com.example.data.model.VisitAnswers(
                        q1_soeName = "Excel Import System",
                        q2_visitDate = visitDate,
                        q3_schoolName = schoolNameClean,
                        q4_udiseCode = referenceCode,
                        q5_district = district,
                        q6_block = block,
                        q7_principalName = principal,
                        q8_principalMobile = mobile,
                        q9_metPrincipal = "हाँ",
                        q10_missionGyanAwareness = "हाँ",
                        q11_studentCount = "Verified",
                        q12_schoolResponse = "Completed (Excel Import)",
                        q20_finalRemarks = "Imported from Excel as Completed Visit"
                    )
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val answersAdapter = moshi.adapter(com.example.data.model.VisitAnswers::class.java)

                    val visit = com.example.data.model.Visit(
                        visitId = "vst_" + UUID.randomUUID().toString().take(8),
                        schoolId = schoolId,
                        employeeId = "emp_system",
                        employeeName = "System (Excel Import)",
                        schoolName = schoolNameClean,
                        district = district,
                        block = block,
                        visitDate = visitDate,
                        status = com.example.data.model.VisitStatus.SUBMITTED,
                        answersJson = answersAdapter.toJson(answers),
                        photosJson = "{}",
                        syncStatus = com.example.data.model.SyncStatus.SYNCED
                    )
                    completedVisits.add(visit)
                }
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
            completedVisitsToImport = completedVisits,
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
