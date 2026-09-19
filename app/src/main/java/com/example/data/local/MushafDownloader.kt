package com.example.data.local

import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import android.content.Context
import com.example.data.model.DownloadState
import com.example.data.model.DownloadStatus
import com.example.util.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.worker.PdfExtractionWorker

class MushafDownloader(
    private val context: Context,
    private val storageManager: StorageManager
) {
    private val client = OkHttpClient()
    private val activeDownloads = mutableMapOf<String, Job>()
    private val pausedDownloads = mutableSetOf<String>()
    
    suspend fun downloadMushaf(
        mushafId: String,
        baseUrl: String,
        totalPages: Int,
        pdfPageOffset: Int = 0,
        onProgress: (DownloadStatus) -> Unit,
        scope: CoroutineScope
    ) {
        val job = scope.launch(Dispatchers.IO) {
            pausedDownloads.remove(mushafId)
            
            val isPdf = baseUrl.endsWith(".pdf")
            if (isPdf) {
                downloadPdfFile(mushafId, baseUrl, totalPages, pdfPageOffset, onProgress)
                return@launch
            }

            val semaphore = Semaphore(3) // Max 3 concurrent downloads
            
            var downloadedCount = storageManager.getDownloadedPagesCount(mushafId)
            
            if (downloadedCount == totalPages) {
                onProgress(DownloadStatus(mushafId, DownloadState.Downloaded, 100, downloadedCount, totalPages))
                return@launch
            }
            
            onProgress(DownloadStatus(mushafId, DownloadState.Downloading, (downloadedCount * 100) / totalPages, downloadedCount, totalPages))
            
            val pagesToDownload = (1..totalPages).filter { !storageManager.isPageDownloaded(mushafId, it) }
            
            val downloadChannel = Channel<Boolean>(Channel.UNLIMITED)
            
            for (page in pagesToDownload) {
                if (!isActive || pausedDownloads.contains(mushafId)) {
                    break
                }
                
                launch {
                    semaphore.withPermit {
                        if (!isActive || pausedDownloads.contains(mushafId)) return@withPermit
                        val success = downloadPage(mushafId, baseUrl, page)
                        downloadChannel.send(success)
                    }
                }
            }
            
            var allSuccessful = true
            var processed = 0
            
            while (processed < pagesToDownload.size && isActive && !pausedDownloads.contains(mushafId)) {
                val success = downloadChannel.receive()
                if (success) {
                    downloadedCount++
                    val progress = (downloadedCount * 100) / totalPages
                    onProgress(DownloadStatus(mushafId, DownloadState.Downloading, progress, downloadedCount, totalPages))
                } else {
                    allSuccessful = false
                }
                processed++
            }
            
            if (pausedDownloads.contains(mushafId)) {
                // Was paused
            } else if (downloadedCount == totalPages) {
                onProgress(DownloadStatus(mushafId, DownloadState.Downloaded, 100, downloadedCount, totalPages))
            } else {
                onProgress(DownloadStatus(mushafId, DownloadState.Failed, (downloadedCount * 100) / totalPages, downloadedCount, totalPages))
            }
        }
        activeDownloads[mushafId] = job
    }
    
    private suspend fun downloadPdfFile(
        mushafId: String,
        url: String,
        totalPages: Int,
        pdfPageOffset: Int,
        onProgress: (DownloadStatus) -> Unit
    ) = withContext(Dispatchers.IO) {
        val destDir = storageManager.getMushafDirectory(mushafId)
        val file = java.io.File(destDir, "mushaf.pdf")
        val tempFile = java.io.File(destDir, "mushaf.pdf.tmp")
        
        try {
            if (file.exists() && file.length() > 0) {
                // Verify if the PDF is readable
                try {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    renderer.close()
                    pfd.close()
                    enqueueExtraction(mushafId, totalPages, pdfPageOffset)
                    onProgress(DownloadStatus(mushafId, DownloadState.Downloaded, 100, totalPages, totalPages))
                    return@withContext
                } catch (e: Exception) {
                    // PDF file is corrupted or incomplete, delete and download again
                    file.delete()
                }
            }
            
            if (tempFile.exists()) {
                tempFile.delete()
            }
            
            onProgress(DownloadStatus(mushafId, DownloadState.Downloading, 0, 0, totalPages))
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body
                if (body != null) {
                    val contentLength = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(tempFile)
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastUpdatePercent = 0
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (pausedDownloads.contains(mushafId)) {
                            outputStream.close()
                            inputStream.close()
                            tempFile.delete()
                            return@withContext
                        }
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        if (contentLength > 0) {
                            val percent = ((totalBytesRead * 100) / contentLength).toInt()
                            if (percent > lastUpdatePercent) {
                                lastUpdatePercent = percent
                                val fakePageCount = (percent * totalPages) / 100
                                onProgress(DownloadStatus(mushafId, DownloadState.Downloading, percent, fakePageCount, totalPages))
                            }
                        }
                    }
                    
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    
                    if (tempFile.renameTo(file)) {
                        enqueueExtraction(mushafId, totalPages, pdfPageOffset)
                        onProgress(DownloadStatus(mushafId, DownloadState.Downloaded, 100, totalPages, totalPages))
                    } else {
                        tempFile.delete()
                        onProgress(DownloadStatus(mushafId, DownloadState.Failed, 0, 0, totalPages))
                    }
                } else {
                    tempFile.delete()
                    onProgress(DownloadStatus(mushafId, DownloadState.Failed, 0, 0, totalPages))
                }
            } else {
                tempFile.delete()
                onProgress(DownloadStatus(mushafId, DownloadState.Failed, 0, 0, totalPages))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) {
                tempFile.delete()
            }
            onProgress(DownloadStatus(mushafId, DownloadState.Failed, 0, 0, totalPages))
        }
    }

    private fun enqueueExtraction(mushafId: String, totalPages: Int, pdfPageOffset: Int) {
        val workData = workDataOf(
            "mushafId" to mushafId,
            "totalPages" to totalPages,
            "offset" to pdfPageOffset
        )
        val request = OneTimeWorkRequestBuilder<PdfExtractionWorker>()
            .setInputData(workData)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private suspend fun downloadPage(mushafId: String, baseUrl: String, page: Int): Boolean = withContext(Dispatchers.IO) {
        var attempts = 0
        while (attempts < 3) {
            try {
                val page3Str = String.format("%03d", page)
                val url = baseUrl.replace("{page}", page.toString()).replace("{page3}", page3Str)
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        val file = storageManager.getPageFile(mushafId, page)
                        val tempFile = java.io.File(file.parent, file.name + ".tmp")
                        val inputStream: java.io.InputStream = body.byteStream()
                        val outputStream = java.io.FileOutputStream(tempFile)
                        
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        tempFile.renameTo(file)
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            attempts++
            delay(1000)
        }
        return@withContext false
    }
    
    suspend fun downloadSinglePage(mushafId: String, baseUrl: String, page: Int): Boolean {
        return downloadPage(mushafId, baseUrl, page)
    }

    fun pauseDownload(mushafId: String) {
        pausedDownloads.add(mushafId)
        activeDownloads[mushafId]?.cancel()
        activeDownloads.remove(mushafId)
    }
    
    fun cancelDownload(mushafId: String) {
        pausedDownloads.remove(mushafId)
        activeDownloads[mushafId]?.cancel()
        activeDownloads.remove(mushafId)
        storageManager.deleteMushaf(mushafId)
    }
}
