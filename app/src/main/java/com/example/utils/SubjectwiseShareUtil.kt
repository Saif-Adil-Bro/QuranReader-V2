package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.SubjectwiseVerse
import java.io.File
import java.io.FileOutputStream

object SubjectwiseShareUtil {

    fun buildVerseShareText(verse: SubjectwiseVerse, categoryName: String): String {
        return buildString {
            append("📖 বিষয়ভিত্তিক কুরআন • ").append(categoryName).append("\n")
            append("📌 ").append(verse.surahName).append(" : ").append(verse.verseNo).append("\n\n")
            if (verse.arabicText.isNotEmpty()) {
                append("আরবি:\n").append(verse.arabicText).append("\n\n")
            }
            append("বাংলা অনুবাদ:\n").append(verse.banglaTranslation).append("\n")
            if (verse.lesson.isNotEmpty()) {
                append("\nশিক্ষা:\n").append(verse.lesson).append("\n")
            }
            append("\n---\n")
            append("📱 ❝কুরআন রিডার❞ অ্যাপ থেকে শেয়ারকৃত।")
        }
    }

    fun copyToClipboard(context: Context, verse: SubjectwiseVerse, categoryName: String) {
        val shareText = buildVerseShareText(verse, categoryName)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Subjectwise Quran Verse", shareText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "আয়াত ও শিক্ষা ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
    }

    fun shareAsText(context: Context, verse: SubjectwiseVerse, categoryName: String) {
        val shareText = buildVerseShareText(verse, categoryName)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "আয়াত শেয়ার করুন (টেক্সট)"))
    }

    fun createPhotoCardPayload(verse: SubjectwiseVerse, categoryName: String): PhotoCardBridge.QuickCardPayload {
        return PhotoCardBridge.fromSubjectwiseVerse(verse, categoryName)
    }

    fun toShortPost(verse: SubjectwiseVerse, categoryName: String): com.example.data.model.ShortPost {
        return PhotoCardBridge.fromSubjectwiseVerse(verse, categoryName).toShortPost()
    }

    private fun measureAndDrawVerse(
        canvas: Canvas?,
        verse: SubjectwiseVerse,
        categoryName: String,
        context: Context
    ): Int {
        val width = 1080
        val margin = 72
        val contentWidth = width - 2 * margin

        val arabicFont = try {
            ResourcesCompat.getFont(context, R.font.scheherazade_new) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val banglaFont = try {
            ResourcesCompat.getFont(context, R.font.solaimanlipi)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val banglaBoldFont = try {
            ResourcesCompat.getFont(context, R.font.solaimanlipi_bold)
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }

        var currentY = 100f

        // 1. Top Category & Reference Pill
        val categoryText = "$categoryName • ${verse.surahName} (${verse.verseNo})"
        val categoryPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 32f
            typeface = banglaBoldFont
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        if (canvas != null) {
            val pillPaint = Paint().apply {
                color = Color.parseColor("#33FFFFFF")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val textWidth = categoryPaint.measureText(categoryText)
            val rect = RectF(
                (width / 2f) - (textWidth / 2f) - 30f,
                currentY - 10f,
                (width / 2f) + (textWidth / 2f) + 30f,
                currentY + 45f
            )
            canvas.drawRoundRect(rect, 30f, 30f, pillPaint)
            canvas.drawText(categoryText, width / 2f, currentY + 28f, categoryPaint)
        }
        currentY += 100f

        // 2. Arabic Text
        if (verse.arabicText.isNotEmpty()) {
            val arabicPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 60f
                typeface = arabicFont
                isAntiAlias = true
            }
            val arabicLayout = StaticLayout.Builder.obtain(
                verse.arabicText,
                0,
                verse.arabicText.length,
                arabicPaint,
                contentWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.4f)
                .setIncludePad(true)
                .build()

            if (canvas != null) {
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                arabicLayout.draw(canvas)
                canvas.restore()
            }
            currentY += arabicLayout.height + 40f

            // Divider
            if (canvas != null) {
                val linePaint = Paint().apply {
                    color = Color.parseColor("#20FFFFFF")
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawLine(margin.toFloat(), currentY, (width - margin).toFloat(), currentY, linePaint)
            }
            currentY += 40f
        }

        // 3. Bangla Translation
        val translationLabelPaint = TextPaint().apply {
            color = Color.parseColor("#00E5FF")
            textSize = 32f
            typeface = banglaBoldFont
            isAntiAlias = true
        }

        if (canvas != null) {
            canvas.drawText("বাংলা অর্থ:", margin.toFloat(), currentY, translationLabelPaint)
        }
        currentY += 45f

        val translationBodyPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 42f
            typeface = banglaFont
            isAntiAlias = true
        }
        val translationLayout = StaticLayout.Builder.obtain(
            verse.banglaTranslation,
            0,
            verse.banglaTranslation.length,
            translationBodyPaint,
            contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.3f)
            .setIncludePad(true)
            .build()

        if (canvas != null) {
            canvas.save()
            canvas.translate(margin.toFloat(), currentY)
            translationLayout.draw(canvas)
            canvas.restore()
        }
        currentY += translationLayout.height + 40f

        // 4. Lesson / Tafsir (if present)
        if (verse.lesson.isNotEmpty()) {
            val lessonLabelPaint = TextPaint().apply {
                color = Color.parseColor("#FFD54F")
                textSize = 32f
                typeface = banglaBoldFont
                isAntiAlias = true
            }

            if (canvas != null) {
                canvas.drawText("শিক্ষা:", margin.toFloat(), currentY, lessonLabelPaint)
            }
            currentY += 45f

            val lessonBodyPaint = TextPaint().apply {
                color = Color.parseColor("#E0E0E0")
                textSize = 38f
                typeface = banglaFont
                isAntiAlias = true
            }
            val lessonLayout = StaticLayout.Builder.obtain(
                verse.lesson,
                0,
                verse.lesson.length,
                lessonBodyPaint,
                contentWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.25f)
                .setIncludePad(true)
                .build()

            if (canvas != null) {
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                lessonLayout.draw(canvas)
                canvas.restore()
            }
            currentY += lessonLayout.height + 40f
        }

        // 5. App Footer / Branding
        if (canvas != null) {
            val linePaint = Paint().apply {
                color = Color.parseColor("#20FFFFFF")
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(margin.toFloat(), currentY, (width - margin).toFloat(), currentY, linePaint)
            currentY += 40f

            val creditPaint = TextPaint().apply {
                color = Color.parseColor("#B0BEC5")
                textSize = 28f
                typeface = banglaBoldFont
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("📱 ❝কুরআন রিডার❞ অ্যাপ থেকে সংগৃহীত", width / 2f, currentY + 30f, creditPaint)
        }
        currentY += 100f

        return currentY.toInt()
    }

    fun shareAsImage(context: Context, verse: SubjectwiseVerse, categoryName: String) {
        try {
            val shareData = IslamicCardTemplate.ShareData(
                badgeTitle = categoryName.ifBlank { "বিষয়ভিত্তিক ক্বুরআন" },
                arabicText = verse.arabicText.takeIf { it.isNotBlank() },
                transliterationText = null,
                translationText = verse.banglaTranslation,
                referenceText = "${verse.surahName} : ${verse.verseNo}"
            )

            val bitmap = IslamicCardTemplate.generateCardBitmap(context, shareData)

            val cacheDir = File(context.cacheDir, "shared_images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, "subjectwise_verse_${verse.id}.png")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "আয়াত শেয়ার করুন (ছবি)"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "ছবি তৈরি করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
