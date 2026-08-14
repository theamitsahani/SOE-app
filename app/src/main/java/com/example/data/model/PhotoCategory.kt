package com.example.data.model

enum class PhotoCategory(
    val categoryId: String,
    val displayName: String,
    val minRequired: Int,
    val supportsVideo: Boolean = false
) {
    SCHOOL_PHOTO("school_photo", "School Photo (विद्यालय फोटो)", 1, false),
    EXPLAINING_APP("explaining_app", "Explaining Our App (ऐप समझाते हुए)", 1, false),
    STUDENTS_SMART_BOARD("students_smart_board", "Students Using Smart Board (स्मार्ट बोर्ड उपयोग)", 1, false),
    PRINCIPAL_PHOTO("principal_photo", "Photo With Principal Sir (प्रधानाचार्य जी के साथ)", 1, false),
    LETTER_PHOTO("letter_photo", "Letter Photo (पत्र/दस्तावेज फोटो)", 1, false),
    OTHER_PHOTOS("other_photos", "Other Photos & Videos (अन्य फोटो व वीडियो - No Limit)", 0, true);

    companion object {
        fun fromId(id: String): PhotoCategory {
            return entries.firstOrNull { 
                it.categoryId == id || 
                it.displayName.equals(id, ignoreCase = true) ||
                it.displayName.startsWith(id, ignoreCase = true) ||
                id.startsWith("Other", ignoreCase = true) && it == OTHER_PHOTOS
            } ?: OTHER_PHOTOS
        }
    }
}

