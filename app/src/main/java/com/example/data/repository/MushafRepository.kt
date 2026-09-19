package com.example.data.repository

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.DownloadStatus
import com.example.data.model.MushafStyle
import com.example.data.local.MushafDownloader
import com.example.util.StorageManager
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MushafRepository(
    private val downloader: MushafDownloader,
    private val storageManager: StorageManager
) {

    fun getAvailableMushafs(): List<MushafStyle> {
        val list = mutableListOf(
            MushafStyle(
                id = "imdadia_hafezi",
                name = "Imdadia Hafezi Quran",
                nameBengali = "ইমদাদিয়া হাফেজী কুরআন",
                description = "Imdadia 15-Line Hafezi Quran PDF",
                descriptionBengali = "ইমদাদিয়া ১৫-লাইন হাফেজী কুরআন (একক ফাইল, সম্পূর্ণ অফলাইন)",
                totalPages = 611,
                fileSizeMB = 30,
                thumbnailUrl = "https://pub-26d400c878304ce889b8454325e14661.r2.dev/imdadia/Screenshot_2026-07-16-05-42-42-398_com.google.android.apps.docs-edit.jpg",
                baseUrl = "https://pub-26d400c878304ce889b8454325e14661.r2.dev/imdadia/imdadia-hafezi-quran3_text_copy.pdf",
                isPdf = true,
                pdfPageOffset = 1
            ),
            MushafStyle(
                id = "hafizi_15line",
                name = "Hafizi 15-Line Quran",
                nameBengali = "হাফেজী ১৫-লাইন কুরআন",
                description = "Standard 15-Line Hafizi Quran PDF",
                descriptionBengali = "স্ট্যান্ডার্ড ১৫-লাইন হাফেজী কুরআন (একক ফাইল, সম্পূর্ণ অফলাইন)",
                totalPages = 611,
                fileSizeMB = 45,
                thumbnailUrl = "https://pub-26d400c878304ce889b8454325e14661.r2.dev/IMG_20260713_174626.jpg",
                baseUrl = "https://pub-26d400c878304ce889b8454325e14661.r2.dev/hafizi-quran-15-line.pdf",
                isPdf = true,
                pdfPageOffset = 1
            ),
            MushafStyle(
                id = "indopak",
                name = "Indo-Pak Script",
                nameBengali = "ইন্দো-পাক লিপি",
                description = "Popular Nastaliq style in India/Pakistan",
                descriptionBengali = "ভারত-পাকিস্তানে জনপ্রিয় নস্তালিক স্টাইল",
                fileSizeMB = 280,
                thumbnailUrl = "https://pub-26d400c878304ce889b8454325e14661.r2.dev/indo-pak/IMG_20260713_192455.jpg",
                baseUrl = "https://android.quran.com/data/width_1024/page{page3}.png"
            )
        )

        val customPdfFile = File(storageManager.getMushafDirectory("custom_pdf"), "mushaf.pdf")
        if (customPdfFile.exists() && customPdfFile.length() > 0) {
            val customSizeMB = (customPdfFile.length() / (1024 * 1024)).toInt()
            list.add(
                0, // Put custom PDF at the top for easy access
                MushafStyle(
                    id = "custom_pdf",
                    name = "Custom Imported Quran",
                    nameBengali = "আপনার পিডিএফ কুরআন",
                    description = "Your custom imported Quran PDF file",
                    descriptionBengali = "আপনার ডিভাইস থেকে আমদানিকৃত পিডিএফ কুরআন",
                    totalPages = 611,
                    fileSizeMB = if (customSizeMB > 0) customSizeMB else 1,
                    thumbnailUrl = "https://cdn.islamic.network/quran/images/1_1.png",
                    baseUrl = "",
                    isPdf = true,
                    pdfPageOffset = 0
                )
            )
        }

        return list
    }

    suspend fun downloadMushaf(
        mushaf: MushafStyle,
        scope: CoroutineScope,
        onProgress: (DownloadStatus) -> Unit
    ) {
        downloader.downloadMushaf(
            mushaf.id,
            mushaf.baseUrl,
            mushaf.totalPages,
            mushaf.pdfPageOffset,
            onProgress,
            scope
        )
    }

    suspend fun downloadSinglePage(mushafId: String, pageNumber: Int): Boolean {
        val style = getAvailableMushafs().find { it.id == mushafId } ?: return false
        if (style.isPdf) return false
        return downloader.downloadSinglePage(mushafId, style.baseUrl, pageNumber)
    }

    fun pauseDownload(mushafId: String) {
        downloader.pauseDownload(mushafId)
    }

    fun cancelDownload(mushafId: String) {
        downloader.cancelDownload(mushafId)
    }

    fun deleteMushaf(mushafId: String): Boolean {
        return storageManager.deleteMushaf(mushafId)
    }

    fun isMushafDownloaded(mushafId: String): Boolean {
        val style = getAvailableMushafs().find { it.id == mushafId }
        if (style?.isPdf == true) {
            val file = File(storageManager.getMushafDirectory(mushafId), "mushaf.pdf")
            return file.exists() && file.length() > 0
        }
        val count = storageManager.getDownloadedPagesCount(mushafId)
        return count >= (style?.totalPages ?: 604)
    }

    fun getDownloadedPagesCount(mushafId: String): Int {
        val style = getAvailableMushafs().find { it.id == mushafId }
        if (style?.isPdf == true) {
            val file = File(storageManager.getMushafDirectory(mushafId), "mushaf.pdf")
            return if (file.exists() && file.length() > 0) style.totalPages else 0
        }
        return storageManager.getDownloadedPagesCount(mushafId)
    }

    fun getMushafPagePath(mushafId: String, pageNumber: Int, customOffset: Int? = null): String? {
        val style = getAvailableMushafs().find { it.id == mushafId }
        val file = storageManager.getPageFile(mushafId, pageNumber)
        
        if (file.exists()) {
            return file.absolutePath
        }

        if (style?.isPdf == true) {
            val pdfFile = File(storageManager.getMushafDirectory(mushafId), "mushaf.pdf")
            if (pdfFile.exists() && pdfFile.length() > 0) {
                val offset = customOffset ?: style.pdfPageOffset
                val renderedFile = renderPdfPageToImage(pdfFile, pageNumber, file, offset)
                if (renderedFile != null) {
                    return renderedFile.absolutePath
                }
            }
        }
        return null
    }

    fun clearRenderedPages(mushafId: String) {
        try {
            val destDir = storageManager.getMushafDirectory(mushafId)
            destDir.listFiles()?.forEach { file ->
                if (file.name != "mushaf.pdf") {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun renderPdfPageToImage(pdfFile: File, pageNumber: Int, outputFile: File, offset: Int): File? {
        try {
            val parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            
            val pdfPageNumber = pageNumber - 1 + offset
            if (pdfPageNumber < 0 || pdfPageNumber >= pdfRenderer.pageCount) {
                pdfRenderer.close()
                parcelFileDescriptor.close()
                return null
            }
            
            val page = pdfRenderer.openPage(pdfPageNumber)
            
            val scale = 2.0f
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            val tempFile = File(outputFile.parent, outputFile.name + ".tmp")
            val out = FileOutputStream(tempFile)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
            }
            out.flush()
            out.close()
            tempFile.renameTo(outputFile)
            
            bitmap.recycle()
            page.close()
            pdfRenderer.close()
            parcelFileDescriptor.close()
            
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun importCustomPdf(pdfInputStream: InputStream): Boolean {
        return try {
            val destDir = storageManager.getMushafDirectory("custom_pdf")
            val destFile = File(destDir, "mushaf.pdf")
            
            // Delete old renders if importing a new one
            destDir.listFiles()?.forEach { file ->
                if (file.name != "mushaf.pdf") {
                    file.delete()
                }
            }
            
            val outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (pdfInputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            outputStream.close()
            pdfInputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
