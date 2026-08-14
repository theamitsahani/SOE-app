package com.example.data.model

data class VisitAnswers(
    val q1_soeName: String = "",
    val q2_visitDate: String = "",
    val q3_schoolName: String = "",
    val q4_udiseCode: String = "",
    val q5_district: String = "",
    val q6_block: String = "",
    val q7_principalName: String = "",
    val q8_principalMobile: String = "",
    val q9_metPrincipal: String = "", // Unselected by default: हाँ, नहीं
    val q10_missionGyanAwareness: String = "", // Unselected by default: हाँ, नहीं, थोड़ी जानकारी थी
    val q11_studentCount: String = "",
    val q12_schoolResponse: String = "", // Unselected by default: बहुत अच्छी, अच्छी, सामान्य, कमजोर
    val q13_bciName: String = "",
    val q13_bciMobile: String = "",
    val q13_bciContactDetails: String = "",
    val q14_whatsappGroupAdded: String = "", // Unselected by default: हाँ, नहीं, लंबित
    val q15_posterInstalled: String = "", // Unselected by default: हाँ, नहीं
    val q16_keyObservations: String = "",
    val q17_problemsOrAssistance: String = "",
    val q18_followupRequired: String = "", // Unselected by default: हाँ, नहीं
    val q20_finalRemarks: String = "",
    val q21_smartClassStatus: String = "", // Unselected by default: बहुत अच्छी, अच्छी, सामान्य, खराब, उपयोग में नहीं है, स्मार्ट क्लास उपलब्ध नहीं है
    val q22_participatingClasses: String = "", // Unchecked by default: e.g. "Class 6th, Class 7th"
    val q23_state: String = "Rajasthan"
)
