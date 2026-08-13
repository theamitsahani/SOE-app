package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VisitAnswers(
    val q1_soeName: String = "",
    val q2_visitDate: String = "",
    val q3_schoolName: String = "",
    val q4_udiseCode: String = "",
    val q5_district: String = "",
    val q6_block: String = "",
    val q7_principalName: String = "",
    val q8_principalMobile: String = "",
    val q9_metPrincipal: String = "हाँ", // हाँ, नहीं
    val q10_missionGyanAwareness: String = "हाँ", // हाँ, नहीं, थोड़ी जानकारी थी
    val q11_studentCount: String = "",
    val q12_schoolResponse: String = "बहुत अच्छी", // बहुत अच्छी, अच्छी, सामान्य, कमजोर
    val q13_bciContactDetails: String = "",
    val q14_whatsappGroupAdded: String = "हाँ", // हाँ, नहीं, लंबित
    val q15_posterInstalled: String = "हाँ", // हाँ, नहीं
    val q16_keyObservations: String = "",
    val q17_problemsOrAssistance: String = "",
    val q18_followupRequired: String = "नहीं", // हाँ, नहीं
    val q20_finalRemarks: String = "",
    val q21_smartClassStatus: String = "बहुत अच्छी" // बहुत अच्छी, अच्छी, सामान्य, खराब, उपयोग में नहीं है, स्मार्ट क्लास उपलब्ध नहीं है
)
