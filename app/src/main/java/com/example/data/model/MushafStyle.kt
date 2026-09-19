package com.example.data.model

data class MushafStyle(
    val id: String,
    val name: String,
    val nameBengali: String,
    val description: String,
    val descriptionBengali: String,
    val totalPages: Int = 604,
    val fileSizeMB: Int,
    val thumbnailUrl: String,
    val baseUrl: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedPages: Int = 0,
    val isPdf: Boolean = false,
    val pdfPageOffset: Int = 0
)
