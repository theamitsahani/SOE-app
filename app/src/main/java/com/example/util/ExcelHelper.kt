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
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.zip.ZipInputStream

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
     * Parses uploaded CSV or native Excel (.xlsx) file.
     * Column Mapping Rules:
     * 1st Row (Header Row): Ignored & Not counted. Reading starts strictly from 2nd row (index 1).
     * Column A (0): State Name
     * Column B (1): District Name
     * Column C (2): School Name (Required: Row is INVALID ONLY if Column C is empty)
     * Column D (3): School Type
     * Column E (4): Village Name
     * Column F (5): Principal Name
     * Column G (6): Block Name
     * Column H (7): Principal Mobile Number
     * Column I (8): Visit Date
     * Column J (9) / Column G: Status -> If "TRUE"/"True"/"1"/"YES"/"COMPLETED", marked as COMPLETED Visit!
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
            val bytes = inputStream.readBytes()
            inputStream.close()

            if (bytes.isEmpty()) {
                return ImportValidationResult(0, 0, 0, 0, emptyList(), emptyList(), listOf("Uploaded file is empty"))
            }

            val parsedRows: List<List<String>> = if (isXlsxZip(bytes)) {
                readXlsxRows(bytes)
            } else {
                readCsvRows(bytes)
            }

            if (parsedRows.size <= 1) {
                return ImportValidationResult(0, 0, 0, 0, emptyList(), emptyList(), listOf("File contains no data rows (only header or empty)"))
            }

            val existingNames = existingSchools.map { cleanText(it.schoolName).lowercase() }.toSet()
            val existingRefCodes = existingSchools.map { cleanText(it.referenceCode) }.filter { it.isNotEmpty() }.toSet()

            // Skip row 0 (1st row is header). Process starting strictly from index 1 (2nd row).
            for (i in 1 until parsedRows.size) {
                val row = parsedRows[i]
                if (row.all { it.isBlank() }) continue

                totalRows++

                fun getCol(idx: Int): String {
                    return if (idx >= 0 && idx < row.size) cleanText(row[idx]) else ""
                }

                val state = getCol(0)         // Column A
                val district = getCol(1)      // Column B
                val rawSchoolName = getCol(2) // Column C
                val type = getCol(3)          // Column D
                val village = getCol(4)       // Column E
                val principal = getCol(5)     // Column F
                val block = getCol(6)         // Column G
                val mobile = getCol(7)        // Column H
                val visitDate = getCol(8)     // Column I
                val statusStr = getCol(9)     // Column J

                // RULE: ONLY if Column C (school name) is empty, consider row INVALID
                if (rawSchoolName.isBlank()) {
                    invalidRows++
                    errors.add("Row ${i + 1}: Column C (School Name) is empty")
                    continue
                }

                // Extract reference code from brackets if present e.g. "GSSS School (8788688)"
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
                    state = state.ifBlank { "Rajasthan" },
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

                // RULE: If Column J or Column G or status string is "TRUE"/"True"/"1"/"YES"/"COMPLETED" -> mark as Completed Visit
                val isCompleted = statusStr.equals("TRUE", ignoreCase = true) ||
                        statusStr.equals("1") ||
                        statusStr.equals("YES", ignoreCase = true) ||
                        statusStr.equals("COMPLETED", ignoreCase = true) ||
                        block.equals("TRUE", ignoreCase = true)

                if (isCompleted) {
                    val answers = com.example.data.model.VisitAnswers(
                        q1_soeName = "Excel Import System",
                        q2_visitDate = visitDate.ifBlank { "Imported" },
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
                        visitDate = visitDate.ifBlank { "Imported" },
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

    /**
     * Sanitizes strings to prevent garbled unicode replacement characters or binary control chars.
     */
    private fun cleanText(input: String): String {
        if (input.isBlank()) return ""
        var cleaned = input.replace("\uFEFF", "") // strip BOM
            .replace("\uFFFD", "")                 // strip unicode replacement character diamond
            .replace(Regex("""[\x00-\x08\x0B\x0C\x0E-\x1F]"""), "") // strip control characters
            .trim()
        return cleaned
    }

    private fun isXlsxZip(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    /**
     * Native parser for Excel .xlsx ZIP archive using ZipInputStream + XmlPullParser.
     */
    private fun readXlsxRows(bytes: ByteArray): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableMapOf<Int, MutableMap<Int, String>>()

        // Step 1: Extract sharedStrings.xml
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("sharedStrings.xml")) {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(zis, "UTF-8")
                    var eventType = parser.eventType
                    var inText = false
                    val currentSb = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "t") inText = true
                            }
                            XmlPullParser.TEXT -> {
                                if (inText) currentSb.append(parser.text)
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "t") inText = false
                                else if (parser.name == "si") {
                                    sharedStrings.add(currentSb.toString().trim())
                                    currentSb.clear()
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zis.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step 2: Extract sheet1.xml (or first worksheet)
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.contains("worksheets/sheet")) {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(zis, "UTF-8")

                    var eventType = parser.eventType
                    var currentRowIdx = -1
                    var currentColIdx = -1
                    var cellType = ""
                    val cellValue = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "row") {
                                    val rAttr = parser.getAttributeValue(null, "r")
                                    currentRowIdx = (rAttr?.toIntOrNull() ?: (currentRowIdx + 2)) - 1
                                    if (!sheetRows.containsKey(currentRowIdx)) {
                                        sheetRows[currentRowIdx] = mutableMapOf()
                                    }
                                } else if (parser.name == "c") {
                                    val rRef = parser.getAttributeValue(null, "r")
                                    cellType = parser.getAttributeValue(null, "t") ?: ""
                                    if (rRef != null) {
                                        currentColIdx = colRefToIndex(rRef)
                                    } else {
                                        currentColIdx++
                                    }
                                    cellValue.clear()
                                }
                            }
                            XmlPullParser.TEXT -> {
                                cellValue.append(parser.text)
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "c") {
                                    val rawVal = cellValue.toString().trim()
                                    var finalVal = rawVal
                                    if (cellType == "s") {
                                        val idx = rawVal.toIntOrNull()
                                        if (idx != null && idx in sharedStrings.indices) {
                                            finalVal = sharedStrings[idx]
                                        }
                                    }
                                    if (currentRowIdx >= 0 && currentColIdx >= 0) {
                                        val rowMap = sheetRows.getOrPut(currentRowIdx) { mutableMapOf() }
                                        rowMap[currentColIdx] = finalVal
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zis.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (sheetRows.isEmpty()) return emptyList()

        val maxRow = sheetRows.keys.maxOrNull() ?: 0
        val result = mutableListOf<List<String>>()
        for (r in 0..maxRow) {
            val rowMap = sheetRows[r]
            if (rowMap == null) {
                result.add(emptyList())
            } else {
                val maxCol = rowMap.keys.maxOrNull() ?: 0
                val rowList = mutableListOf<String>()
                for (c in 0..maxCol) {
                    rowList.add(rowMap[c] ?: "")
                }
                result.add(rowList)
            }
        }
        return result
    }

    private fun colRefToIndex(ref: String): Int {
        val colLetters = ref.takeWhile { it.isLetter() }.uppercase()
        var idx = 0
        for (ch in colLetters) {
            idx = idx * 26 + (ch - 'A' + 1)
        }
        return (idx - 1).coerceAtLeast(0)
    }

    /**
     * Reads CSV content handling UTF-8, UTF-8 BOM, UTF-16, and Windows-1252 encodings.
     */
    private fun readCsvRows(bytes: ByteArray): List<List<String>> {
        var text = String(bytes, Charsets.UTF_8)
        if (text.contains("\uFFFD")) {
            // Try Windows-1252 or UTF-16 if UTF-8 resulted in invalid chars
            val textW1252 = String(bytes, charset("Windows-1252"))
            if (!textW1252.contains("\uFFFD")) {
                text = textW1252
            }
        }
        text = text.replace("\uFEFF", "") // remove BOM

        val lines = text.lines()
        val result = mutableListOf<List<String>>()
        for (line in lines) {
            if (line.isBlank()) continue
            result.add(parseCsvLine(line))
        }
        return result
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
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
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
