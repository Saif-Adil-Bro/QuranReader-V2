package com.example.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.util.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfExtractionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val mushafId = inputData.getString("mushafId") ?: return@withContext Result.failure()
        val totalPages = inputData.getInt("totalPages", 0)
        val offset = inputData.getInt("offset", 0)

        if (totalPages <= 0) return@withContext Result.failure()

        val storageManager = StorageManager(applicationContext)
        val pdfFile = File(storageManager.getMushafDirectory(mushafId), "mushaf.pdf")

        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            return@withContext Result.failure()
        }

        try {
            val parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)

            for (pageNumber in 1..totalPages) {
                // If the worker is cancelled, stop extracting
                if (isStopped) {
                    break
                }

                val outputFile = storageManager.getPageFile(mushafId, pageNumber)
                if (outputFile.exists() && outputFile.length() > 0) {
                    continue // Skip if already extracted
                }

                val pdfPageNumber = pageNumber - 1 + offset
                if (pdfPageNumber < 0 || pdfPageNumber >= pdfRenderer.pageCount) {
                    continue
                }

                extractPage(pdfRenderer, pdfPageNumber, outputFile)
            }

            pdfRenderer.close()
            parcelFileDescriptor.close()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun extractPage(pdfRenderer: PdfRenderer, pdfPageNumber: Int, outputFile: File) {
        try {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
