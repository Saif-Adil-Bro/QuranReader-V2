package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val TajweedLegend = mapOf(
    "ghunnah" to Pair("গুন্নাহ (Ghunnah)", Color(0xFFE53935)), // Red
    "madda_normal" to Pair("মাদ (Madd Normal)", Color(0xFF1E88E5)), // Blue
    "madda_permissible" to Pair("মাদ জায়েজ (Madd Permissible)", Color(0xFF1976D2)), // Blue
    "madda_obligatory" to Pair("মাদ ওয়াজিব (Madd Obligatory)", Color(0xFF0D47A1)), // Dark Blue
    "madda_necessary" to Pair("মাদ লাজিম (Madd Necessary)", Color(0xFF002171)), // Very Dark Blue
    "qalaqah" to Pair("কলকলাহ (Qalqalah)", Color(0xFFFDD835)), // Yellow
    "ikhafa" to Pair("ইখফা (Ikhfa)", Color(0xFF43A047)), // Green
    "idgham_ghunnah" to Pair("ইদগাম গুন্নাহ (Idgham Ghunnah)", Color(0xFF8E24AA)), // Purple
    "idgham_wo_ghunnah" to Pair("গুন্নাহ ছাড়া ইদগام", Color(0xFFBA68C8)), // Light Purple
    "laam_shamsiyah" to Pair("লাম শামসিয়াহ (Laam Shamsiyah)", Color(0xFF757575)), // Gray
    "ham_wasl" to Pair("হামযাতুল ওয়াসল (Hamzatul Wasl)", Color(0xFFBDBDBD)), // Light Gray
    "ikhafa_shafawi" to Pair("ইখফা শাফাওয়ী (Ikhfa Shafawi)", Color(0xFF00ACC1)), // Cyan
    "idgham_shafawi" to Pair("ইদগাম শাফাওয়ী (Idgham Shafawi)", Color(0xFF3949AB)), // Indigo
    "iqlab" to Pair("ইকলাব (Iqlab)", Color(0xFFFB8C00)), // Orange
    "slnt" to Pair("উহ্য অক্ষর (Silent)", Color(0xFF9E9E9E)) // Gray
)

private val TajweedColorMap: Map<String, Color> = buildMap {
    TajweedLegend.forEach { (k, v) -> put(k, v.second) }
    // Aliases
    put("qalqala", Color(0xFFFDD835))
    put("qalaqa", Color(0xFFFDD835))
    put("ikhfa", Color(0xFF43A047))
    put("ikhfa_shafawi", Color(0xFF00ACC1))
    put("madd_normal", Color(0xFF1E88E5))
    put("madd_permissible", Color(0xFF1976D2))
    put("madd_obligatory", Color(0xFF0D47A1))
    put("madd_necessary", Color(0xFF002171))
    put("madd_2", Color(0xFF1E88E5))
    put("madd_246", Color(0xFF1976D2))
    put("madd_6", Color(0xFF0D47A1))
    put("idgham_w_ghunnah", Color(0xFF8E24AA))
    put("idgham_with_ghunnah", Color(0xFF8E24AA))
    put("idgham_without_ghunnah", Color(0xFFBA68C8))
    put("silent", Color(0xFF9E9E9E))
}

val TajweedColors = TajweedColorMap
