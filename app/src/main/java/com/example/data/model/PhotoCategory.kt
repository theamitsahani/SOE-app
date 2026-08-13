package com.example.data.model

enum class PhotoCategory(val categoryId: String, val displayName: String, val minRequired: Int) {
    SCHOOL_PHOTO("school_photo", "School Photo", 1),
    EXPLAINING_APP("explaining_app", "Explaining Our App", 1),
    STUDENTS_SMART_BOARD("students_smart_board", "Students Using Smart Board", 1),
    PRINCIPAL_PHOTO("principal_photo", "Photo With Principal Sir", 1),
    LETTER_PHOTO("letter_photo", "Letter Photo", 1),
    OTHER_PHOTOS("other_photos", "Other Photos", 0);

    companion object {
        fun fromId(id: String): PhotoCategory {
            return entries.firstOrNull { it.categoryId == id || it.displayName.equals(id, ignoreCase = true) }
                ?: OTHER_PHOTOS
        }
    }
}
