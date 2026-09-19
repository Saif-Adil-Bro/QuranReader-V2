package com.example.util

import android.content.Context
import java.io.File
import kotlin.math.log10
import kotlin.math.pow

class StorageManager(private val context: Context) {
    fun getMushafDirectory(mushafId: String): File {
        val dir = File(context.filesDir, "mushafs/$mushafId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun getPageFile(mushafId: String, pageNumber: Int): File {
        val dir = getMushafDirectory(mushafId)
        return File(dir, "$pageNumber.webp")
    }
    
    fun isPageDownloaded(mushafId: String, pageNumber: Int): Boolean {
        return getPageFile(mushafId, pageNumber).exists()
    }
    
    fun getDownloadedPagesCount(mushafId: String): Int {
        val dir = getMushafDirectory(mushafId)
        return dir.listFiles()?.count { it.extension == "webp" } ?: 0
    }
    
    fun getTotalStorageUsed(): Long {
        val rootDir = File(context.filesDir, "mushafs")
        return getFolderSize(rootDir)
    }
    
    fun getMushafStorageSize(mushafId: String): Long {
        return getFolderSize(getMushafDirectory(mushafId))
    }
    
    fun deleteMushaf(mushafId: String): Boolean {
        val dir = getMushafDirectory(mushafId)
        return dir.deleteRecursively()
    }
    
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
    
    private fun getFolderSize(folder: File): Long {
        var length = 0L
        val files = folder.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isFile) {
                    length += file.length()
                } else {
                    length += getFolderSize(file)
                }
            }
        }
        return length
    }
}
