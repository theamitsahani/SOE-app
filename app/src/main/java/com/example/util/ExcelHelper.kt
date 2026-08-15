package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.School
import com.example.data.model.Visit
import com.example.ui.admin.PhotoGridItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

            // Skip row 0 (1st row is header). Process starting strictly from index 1 (2nd row).
            for (i in 1 until parsedRows.size) {
                val row = parsedRows[i]
                if (row.all { it.isBlank() }) continue

                totalRows++

                fun getCol(idx: Int): String {
                    return if (idx >= 0 && idx < row.size) cleanText(row[idx]) else ""
                }

                val stateName = getCol(0)         // Column A: State Name
                val districtName = getCol(1)      // Column B: District Name
                val schoolName = getCol(2)        // Column C: School Name (Required)
                val schoolType = getCol(3)        // Column D: School Type
                val villageName = getCol(4)       // Column E: Village Name
                val principalName = getCol(5)     // Column F: Principal Name
                val blockName = getCol(6)         // Column G: Block Name
                val principalMobile = getCol(7)   // Column H: Principal Mobile Number
                val visitDate = getCol(8)         // Column I: Visit Date
                val statusStr = getCol(9).ifBlank { getCol(6) } // Status string if present

                // RULE: ONLY if Column C (school name) is empty, consider row INVALID
                if (schoolName.isBlank()) {
                    invalidRows++
                    errors.add("Row ${i + 1}: Column C (School Name) is empty")
                    continue
                }

                val existingSchool = existingSchools.find { 
                    it.schoolName.trim().equals(schoolName.trim(), ignoreCase = true) && 
                    (districtName.isBlank() || it.districtName.trim().equals(districtName.trim(), ignoreCase = true))
                }
                val isDuplicate = existingSchool != null
                if (isDuplicate) {
                    duplicateRows++
                }

                val schoolId = existingSchool?.schoolId ?: ("sch_" + Math.abs((schoolName.trim().lowercase() + "_" + districtName.trim().lowercase()).hashCode()).toString(36))

                val school = School(
                    schoolId = schoolId,
                    stateName = stateName.ifBlank { "Rajasthan" },
                    districtName = districtName,
                    schoolName = schoolName, // Exact school name as provided, preserving brackets/numbers
                    schoolType = schoolType,
                    villageName = villageName,
                    principalName = principalName,
                    blockName = blockName,
                    principalMobile = principalMobile,
                    visitDate = visitDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                schools.add(school)
                validRows++

                // Check completed visit status
                val isCompleted = statusStr.equals("TRUE", ignoreCase = true) ||
                        statusStr.equals("1") ||
                        statusStr.equals("YES", ignoreCase = true) ||
                        statusStr.equals("COMPLETED", ignoreCase = true)

                if (isCompleted) {
                    val answers = com.example.data.model.VisitAnswers(
                        q1_soeName = "Excel Import System",
                        q2_visitDate = visitDate.ifBlank { "Imported" },
                        q3_schoolName = schoolName,
                        q4_udiseCode = "",
                        q5_district = districtName,
                        q6_block = blockName,
                        q7_principalName = principalName,
                        q8_principalMobile = principalMobile,
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
                        schoolName = schoolName,
                        district = districtName,
                        block = blockName,
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
     * Sanitizes strings while preserving Unicode Hindi Devanagari text intact.
     */
    private fun cleanText(input: String): String {
        if (input.isBlank()) return ""
        return input.replace("\uFEFF", "").trim()
    }

    private fun isXlsxZip(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    /**
     * Native parser for Excel .xlsx ZIP archive using in-memory entry reading + XmlPullParser.
     */
    private fun readXlsxRows(bytes: ByteArray): List<List<String>> {
        var sharedStringsXml: String? = null
        var sheetXml: String? = null

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name.endsWith("sharedstrings.xml")) {
                        sharedStringsXml = String(zis.readBytes(), Charsets.UTF_8)
                    } else if (name.contains("worksheets/sheet1.xml") || (sheetXml == null && name.contains("worksheets/sheet"))) {
                        sheetXml = String(zis.readBytes(), Charsets.UTF_8)
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sharedStrings = mutableListOf<String>()
        if (!sharedStringsXml.isNullOrEmpty()) {
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(java.io.StringReader(sharedStringsXml))

                var eventType = parser.eventType
                var inTextTag = false
                val currentSb = StringBuilder()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name ?: ""
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName.equals("si", ignoreCase = true)) {
                                currentSb.clear()
                            } else if (tagName.equals("t", ignoreCase = true)) {
                                inTextTag = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inTextTag) {
                                currentSb.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName.equals("t", ignoreCase = true)) {
                                inTextTag = false
                            } else if (tagName.equals("si", ignoreCase = true)) {
                                sharedStrings.add(currentSb.toString())
                                currentSb.clear()
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val sheetRows = mutableMapOf<Int, MutableMap<Int, String>>()
        if (!sheetXml.isNullOrEmpty()) {
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(java.io.StringReader(sheetXml))

                var eventType = parser.eventType
                var currentRowIdx = -1
                var currentColIdx = -1
                var cellType = ""
                var inValueTag = false
                var inInlineTextTag = false
                val cellValueSb = StringBuilder()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name ?: ""
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName.equals("row", ignoreCase = true)) {
                                val rAttr = parser.getAttributeValue(null, "r")
                                currentRowIdx = (rAttr?.toIntOrNull() ?: (currentRowIdx + 2)) - 1
                                if (!sheetRows.containsKey(currentRowIdx)) {
                                    sheetRows[currentRowIdx] = mutableMapOf()
                                }
                            } else if (tagName.equals("c", ignoreCase = true)) {
                                val rRef = parser.getAttributeValue(null, "r")
                                cellType = parser.getAttributeValue(null, "t") ?: ""
                                currentColIdx = if (!rRef.isNullOrBlank()) colRefToIndex(rRef) else (currentColIdx + 1)
                                cellValueSb.clear()
                            } else if (tagName.equals("v", ignoreCase = true)) {
                                inValueTag = true
                            } else if (tagName.equals("t", ignoreCase = true)) {
                                inInlineTextTag = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inValueTag || inInlineTextTag) {
                                cellValueSb.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName.equals("v", ignoreCase = true)) {
                                inValueTag = false
                            } else if (tagName.equals("t", ignoreCase = true)) {
                                inInlineTextTag = false
                            } else if (tagName.equals("c", ignoreCase = true)) {
                                val rawVal = cellValueSb.toString().trim()
                                var finalVal = rawVal
                                if (cellType.equals("s", ignoreCase = true)) {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
     * Reads CSV content handling UTF-8, UTF-8 BOM, and UTF-16 encodings.
     */
    private fun readCsvRows(bytes: ByteArray): List<List<String>> {
        val text = when {
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> {
                String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            }
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> {
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            }
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> {
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            }
            else -> {
                String(bytes, Charsets.UTF_8)
            }
        }

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
     * Exports visits to a clean formatted PDF document.
     */
    fun exportVisitsToPdf(context: Context, visits: List<Visit>): File {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val margin = 36f
        
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1E1B4B")
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val subTitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#475569")
            textSize = 10f
            isAntiAlias = true
        }
        
        val headerBgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#4F46E5")
            style = android.graphics.Paint.Style.FILL
        }
        
        val headerTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val cellTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1E293B")
            textSize = 9f
            isAntiAlias = true
        }
        
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        
        val altRowPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F8FAFC")
            style = android.graphics.Paint.Style.FILL
        }

        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(canvas: android.graphics.Canvas) {
            canvas.drawText("SOE School Visit Reports Summary", margin, 45f, titlePaint)
            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            val subtitle = "Total Reports: ${visits.size}  |  Generated On: $dateStr"
            canvas.drawText(subtitle, margin, 62f, subTitlePaint)
            
            val tableTop = 75f
            val tableHeight = 22f
            canvas.drawRect(margin, tableTop, pageWidth - margin, tableTop + tableHeight, headerBgPaint)
            
            var x = margin + 5f
            canvas.drawText("#", x, tableTop + 15f, headerTextPaint)
            x += 25f
            canvas.drawText("Date", x, tableTop + 15f, headerTextPaint)
            x += 65f
            canvas.drawText("School Name", x, tableTop + 15f, headerTextPaint)
            x += 145f
            canvas.drawText("District", x, tableTop + 15f, headerTextPaint)
            x += 80f
            canvas.drawText("Block", x, tableTop + 15f, headerTextPaint)
            x += 70f
            canvas.drawText("Officer", x, tableTop + 15f, headerTextPaint)
            x += 80f
            canvas.drawText("Status", x, tableTop + 15f, headerTextPaint)
        }

        drawHeader(canvas)

        var y = 102f
        val rowHeight = 22f

        for ((index, visit) in visits.withIndex()) {
            if (y + rowHeight > pageHeight - 40f) {
                subTitlePaint.textAlign = android.graphics.Paint.Align.RIGHT
                canvas.drawText("Page $pageNumber", pageWidth - margin, pageHeight - 20f, subTitlePaint)
                subTitlePaint.textAlign = android.graphics.Paint.Align.LEFT

                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                y = 102f
            }

            if (index % 2 == 1) {
                canvas.drawRect(margin, y - 14f, pageWidth - margin, y + 8f, altRowPaint)
            }
            canvas.drawLine(margin, y + 8f, pageWidth - margin, y + 8f, borderPaint)

            var x = margin + 5f
            canvas.drawText("${index + 1}", x, y, cellTextPaint)
            x += 25f
            canvas.drawText(visit.visitDate.take(10), x, y, cellTextPaint)
            x += 65f
            canvas.drawText(visit.schoolName.take(22), x, y, cellTextPaint)
            x += 145f
            canvas.drawText(visit.district.take(12), x, y, cellTextPaint)
            x += 80f
            canvas.drawText(visit.block.take(10), x, y, cellTextPaint)
            x += 70f
            canvas.drawText(visit.employeeName.take(12), x, y, cellTextPaint)
            x += 80f
            canvas.drawText(visit.status.name.take(10), x, y, cellTextPaint)

            y += rowHeight
        }

        subTitlePaint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("Page $pageNumber", pageWidth - margin, pageHeight - 20f, subTitlePaint)
        subTitlePaint.textAlign = android.graphics.Paint.Align.LEFT

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "SOE_Visit_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        shareFile(context, file, "SOE Visit Reports PDF", "application/pdf")
        return file
    }

    /**
     * Exports visits to a native Excel spreadsheet (.xlsx).
     */
    fun exportVisitsToExcel(context: Context, visits: List<Visit>): File {
        val file = File(context.cacheDir, "SOE_Visit_Report_${System.currentTimeMillis()}.xlsx")
        
        val headers = listOf(
            "Visit ID", "Date", "Employee", "District", "Block", "School Name", 
            "Reference Code", "Principal Name", "Principal Mobile", "Met Principal", 
            "Mission Gyan Knowledge", "Student Attendance", "School Response", 
            "BCI Name", "BCI Mobile", "BCI Full Details", "WhatsApp Group Status", 
            "Poster Installed", "Key Observations", "Problems/Help Required", 
            "Follow-up Needed", "Smart Class Status", "Final Remarks"
        )

        val stringList = mutableListOf<String>()
        val stringMap = mutableMapOf<String, Int>()

        fun getSharedStringIndex(str: String): Int {
            return stringMap.getOrPut(str) {
                val idx = stringList.size
                stringList.add(str)
                idx
            }
        }

        headers.forEach { getSharedStringIndex(it) }

        val rowsData = mutableListOf<List<Int>>()
        for (v in visits) {
            val a = parseVisitAnswers(v.answersJson)
            val bciName = a.q13_bciName.ifBlank {
                if (a.q13_bciContactDetails.contains("-")) a.q13_bciContactDetails.substringBefore("-").trim()
                else a.q13_bciContactDetails
            }
            val bciMobile = a.q13_bciMobile.ifBlank {
                if (a.q13_bciContactDetails.contains("-")) a.q13_bciContactDetails.substringAfter("-").trim()
                else ""
            }

            val rowVals = listOf(
                v.visitId, v.visitDate, v.employeeName, v.district, v.block,
                v.schoolName, a.q4_udiseCode, a.q7_principalName, a.q8_principalMobile,
                a.q9_metPrincipal, a.q10_missionGyanAwareness, a.q11_studentCount, a.q12_schoolResponse,
                bciName, bciMobile, a.q13_bciContactDetails, a.q14_whatsappGroupAdded,
                a.q15_posterInstalled, a.q16_keyObservations, a.q17_problemsOrAssistance,
                a.q18_followupRequired, a.q21_smartClassStatus, a.q20_finalRemarks
            )
            val rowIndices = rowVals.map { getSharedStringIndex(it) }
            rowsData.add(rowIndices)
        }

        val sharedStringsXml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"${stringList.size}\" uniqueCount=\"${stringList.size}\">\n")
            for (str in stringList) {
                val escaped = str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
                append("  <si><t>$escaped</t></si>\n")
            }
            append("</sst>")
        }

        val sheet1Xml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
            append("  <sheetData>\n")

            append("    <row r=\"1\">\n")
            headers.indices.forEach { colIdx ->
                val colRef = getColumnLetter(colIdx) + "1"
                append("      <c r=\"$colRef\" t=\"s\"><v>$colIdx</v></c>\n")
            }
            append("    </row>\n")

            rowsData.forEachIndexed { rIdx, rowIndices ->
                val rowNum = rIdx + 2
                append("    <row r=\"$rowNum\">\n")
                rowIndices.forEachIndexed { cIdx, strIdx ->
                    val colRef = getColumnLetter(cIdx) + rowNum
                    append("      <c r=\"$colRef\" t=\"s\"><v>$strIdx</v></c>\n")
                }
                append("    </row>\n")
            }

            append("  </sheetData>\n")
            append("</worksheet>")
        }

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Visit Reports" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            fun addZipEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addZipEntry("[Content_Types].xml", contentTypesXml)
            addZipEntry("_rels/.rels", relsXml)
            addZipEntry("xl/workbook.xml", workbookXml)
            addZipEntry("xl/_rels/workbook.xml.rels", workbookRelsXml)
            addZipEntry("xl/sharedStrings.xml", sharedStringsXml)
            addZipEntry("xl/worksheets/sheet1.xml", sheet1Xml)
        }

        shareFile(context, file, "SOE Visit Reports Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        return file
    }

    private fun getColumnLetter(colIndex: Int): String {
        var temp = colIndex
        var colName = ""
        while (temp >= 0) {
            colName = ('A' + (temp % 26)).toString() + colName
            temp = temp / 26 - 1
        }
        return colName
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
                    "Met Principal,Mission Gyan Knowledge,Student Attendance,School Response,BCI Name,BCI Mobile,BCI Full Details,WhatsApp Group Status," +
                    "Poster Installed,Key Observations,Problems/Help Required,Follow-up Needed,Smart Class Status,Final Remarks\n"
        )

        for (v in visits) {
            val a = parseVisitAnswers(v.answersJson)
            fun sanitize(str: String): String {
                val escaped = str.replace("\"", "\"\"")
                return "\"$escaped\""
            }

            val bciName = a.q13_bciName.ifBlank {
                if (a.q13_bciContactDetails.contains("-")) a.q13_bciContactDetails.substringBefore("-").trim()
                else a.q13_bciContactDetails
            }
            val bciMobile = a.q13_bciMobile.ifBlank {
                if (a.q13_bciContactDetails.contains("-")) a.q13_bciContactDetails.substringAfter("-").trim()
                else ""
            }

            writer.write(
                "${sanitize(v.visitId)},${sanitize(v.visitDate)},${sanitize(v.employeeName)},${sanitize(v.district)},${sanitize(v.block)}," +
                        "${sanitize(v.schoolName)},${sanitize(a.q4_udiseCode)},${sanitize(a.q7_principalName)},${sanitize(a.q8_principalMobile)}," +
                        "${sanitize(a.q9_metPrincipal)},${sanitize(a.q10_missionGyanAwareness)},${sanitize(a.q11_studentCount)},${sanitize(a.q12_schoolResponse)}," +
                        "${sanitize(bciName)},${sanitize(bciMobile)},${sanitize(a.q13_bciContactDetails)},${sanitize(a.q14_whatsappGroupAdded)},${sanitize(a.q15_posterInstalled)}," +
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

    /**
     * Downloads/reads and packages filtered photos into a true .zip archive based on active filters.
     */
    suspend fun exportPhotosAsZip(
        context: Context,
        photos: List<PhotoGridItem>,
        filterDescription: String,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val cleanFilter = filterDescription.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
        val zipFile = File(context.cacheDir, "SOE_Photos_${cleanFilter}_${System.currentTimeMillis()}.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Write an Index / Summary txt inside the zip
            val summaryText = buildString {
                appendLine("=====================================================")
                appendLine("SOE School Visit Photo Gallery Archive")
                appendLine("Generated: ${java.util.Date()}")
                appendLine("Active Filter: $filterDescription")
                appendLine("Total Filtered Photos: ${photos.size}")
                appendLine("=====================================================\n")
                appendLine("INDEX OF ARCHIVED PHOTOS:")
                photos.forEachIndexed { index, item ->
                    appendLine("${index + 1}. School: ${item.schoolName}")
                    appendLine("   Category: ${item.categoryName}")
                    appendLine("   Location: ${item.state} • ${item.district} • ${item.block}")
                    appendLine("   Visit Date: ${item.date}")
                    appendLine("   Source: ${item.url}")
                    appendLine("-----------------------------------------------------")
                }
            }
            val summaryEntry = ZipEntry("Photos_Summary_Index.txt")
            zos.putNextEntry(summaryEntry)
            zos.write(summaryText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Fetch and package each image/media into the zip
            val usedEntryNames = mutableSetOf<String>()

            photos.forEachIndexed { index, item ->
                withContext(Dispatchers.Main) {
                    onProgress(index + 1, photos.size)
                }
                try {
                    val bytes = readMediaBytes(context, item.url)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val ext = getExtensionFromUrl(item.url)
                        val safeSchool = item.schoolName.replace(Regex("[^a-zA-Z0-9_ -]"), "_").trim().ifBlank { "School" }
                        val safeCategory = item.categoryName.replace(Regex("[^a-zA-Z0-9_ -]"), "_").trim().ifBlank { "Photo" }
                        val safeDistrict = item.district.replace(Regex("[^a-zA-Z0-9_ -]"), "_").trim().ifBlank { "District" }

                        var entryName = "$safeDistrict/$safeSchool/${safeCategory}_${index + 1}.$ext"
                        if (usedEntryNames.contains(entryName)) {
                            entryName = "$safeDistrict/$safeSchool/${safeCategory}_${index + 1}_${System.currentTimeMillis() % 1000}.$ext"
                        }
                        usedEntryNames.add(entryName)

                        val entry = ZipEntry(entryName)
                        zos.putNextEntry(entry)
                        zos.write(bytes)
                        zos.closeEntry()
                    }
                } catch (e: Exception) {
                    Log.e("ExcelHelper", "Failed to add photo #${index + 1} (${item.url}) to zip", e)
                }
            }
        }

        withContext(Dispatchers.Main) {
            shareFile(context, zipFile, "SOE Photos Archive ($filterDescription)", "application/zip")
        }
        zipFile
    }

    private fun readMediaBytes(context: Context, urlOrPath: String): ByteArray? {
        return try {
            when {
                urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://") -> {
                    val conn = java.net.URL(urlOrPath).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 12000
                    conn.readTimeout = 15000
                    conn.instanceFollowRedirects = true
                    conn.inputStream.use { it.readBytes() }
                }
                urlOrPath.startsWith("content://") -> {
                    val uri = Uri.parse(urlOrPath)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                urlOrPath.startsWith("file://") -> {
                    val file = File(Uri.parse(urlOrPath).path ?: "")
                    if (file.exists()) file.readBytes() else null
                }
                else -> {
                    val file = File(urlOrPath)
                    if (file.exists()) file.readBytes() else null
                }
            }
        } catch (e: Exception) {
            Log.e("ExcelHelper", "Error reading media bytes for $urlOrPath", e)
            null
        }
    }

    private fun getExtensionFromUrl(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".png") -> "png"
            lower.contains(".webp") -> "webp"
            lower.contains(".mp4") -> "mp4"
            lower.contains(".mov") -> "mov"
            lower.contains(".3gp") -> "3gp"
            else -> "jpg"
        }
    }
}
