package com.example.data.model

sealed class DownloadState {
    object NotDownloaded : DownloadState()
    object Downloading : DownloadState()
    object Downloaded : DownloadState()
    object Failed : DownloadState()
}

data class DownloadStatus(
    val mushafId: String,
    val state: DownloadState,
    val progress: Int = 0,
    val downloadedPages: Int = 0,
    val totalPages: Int = 604
)
