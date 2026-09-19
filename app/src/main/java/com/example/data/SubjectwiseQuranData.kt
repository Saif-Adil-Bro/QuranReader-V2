package com.example.data

data class SubjectwiseVerse(
    val id: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val verseNo: String,
    val arabicText: String = "",
    val banglaTranslation: String,
    val lesson: String = ""
)

data class SubjectwiseTopic(
    val topicId: Int,
    val titleBn: String,
    val verses: List<SubjectwiseVerse>
)

data class SubjectwiseCategory(
    val categoryId: Int,
    val categoryNameBn: String,
    val icon: String,
    val topics: List<SubjectwiseTopic>
)

object SubjectwiseQuranData {
    val categories: List<SubjectwiseCategory> = emptyList()
}
