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
import com.example.data.DuaItem
import java.io.File
import java.io.FileOutputStream

object DuaShareUtil {

    fun buildDuaShareText(dua: DuaItem): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val banglaNumber = dua.id.toString().map { char ->
            if (char.isDigit()) banglaDigits[char - '0'] else char
        }.joinToString("")

        return buildString {
            append("✨ ").append(dua.title).append(" ✨\n")
            append("দোয়া নং: ").append(banglaNumber).append("\n\n")
            
            dua.segments.forEachIndexed { index, segment ->
                if (index > 0) {
                    append("\n-------------------\n\n")
                }
                if (segment.arabic.isNotEmpty() && segment.arabic != "null") {
                    append("আরবি:\n").append(segment.arabic).append("\n\n")
                }
                if (segment.transliteration.isNotEmpty() && segment.transliteration != "null") {
                    append("উচ্চারণ:\n").append(segment.transliteration).append("\n\n")
                }
                if (segment.translation.isNotEmpty() && segment.translation != "null") {
                    append("অর্থ:\n").append(segment.translation).append("\n\n")
                }
                if (segment.bottom.isNotEmpty() && segment.bottom != "null") {
                    val trimmed = segment.bottom.trim()
                    val contextText = if (trimmed.startsWith("দোয়ার প্রেক্ষাপট") || trimmed.startsWith("দোয়ার প্রেক্ষাপট")) {
                        trimmed
                    } else {
                        "দোয়ার প্রেক্ষাপট: ${segment.bottom}"
                    }
                    append(contextText).append("\n\n")
                }
                if (segment.reference.isNotEmpty() && segment.reference != "null") {
                    append("সূত্র: ").append(segment.reference).append("\n")
                }
            }
            append("\n---\n")
            append("📱 ❝কুরআন রিডার❞ অ্যাপ থেকে শেয়ারকৃত।")
        }
    }

    fun copyToClipboard(context: Context, dua: DuaItem) {
        val shareText = buildDuaShareText(dua)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Dua Text", shareText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "দোয়া ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
    }

    fun shareAsText(context: Context, dua: DuaItem) {
        val shareText = buildDuaShareText(dua)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "দোয়া শেয়ার করুন (টেক্সট)"))
    }

    fun createPhotoCardPayload(dua: DuaItem): PhotoCardBridge.QuickCardPayload {
        return PhotoCardBridge.fromDua(dua)
    }

    fun toShortPost(dua: DuaItem): com.example.data.model.ShortPost {
        return PhotoCardBridge.fromDua(dua).toShortPost()
    }

    private fun measureAndDrawDua(canvas: Canvas?, dua: DuaItem, context: Context): Int {
        val width = 1080
        val margin = 72
        val contentWidth = width - 2 * margin
        
        // Load custom fonts
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

        var currentY = 100f // Starting Y coordinate

        // 1. Draw top category pill "আজকের দোয়া"
        val categoryText = "আজকের দোয়া"
        val categoryPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = banglaBoldFont
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        if (canvas != null) {
            // Draw elegant pill background for "আজকের দোয়া"
            val pillPaint = Paint().apply {
                color = Color.parseColor("#33FFFFFF") // Semi-transparent white
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
        currentY += 100f // space below category

        // 2. Draw Title
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val banglaNumber = dua.id.toString().map { char ->
            if (char.isDigit()) banglaDigits[char - '0'] else char
        }.joinToString("")
        val titleText = "[$banglaNumber] ${dua.title}"
        
        val titlePaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 48f
            typeface = banglaBoldFont
            isAntiAlias = true
        }
        
        val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(true)
            .build()

        if (canvas != null) {
            canvas.save()
            canvas.translate(margin.toFloat(), currentY)
            titleLayout.draw(canvas)
            canvas.restore()
        }
        currentY += titleLayout.height + 40f

        // 3. Draw divider line under title
        if (canvas != null) {
            val linePaint = Paint().apply {
                color = Color.parseColor("#20FFFFFF") // Faint white line
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(margin.toFloat(), currentY, (width - margin).toFloat(), currentY, linePaint)
        }
        currentY += 50f // gap after divider

        // 4. Draw Segments
        val labelPaint = TextPaint().apply {
            color = Color.parseColor("#00E5FF") // High-contrast bright cyan
            textSize = 34f
            typeface = banglaBoldFont
            isAntiAlias = true
        }

        val arabicPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 68f
            typeface = arabicFont
            isAntiAlias = true
        }

        val translationPaint = TextPaint().apply {
            color = Color.parseColor("#EAEAEA")
            textSize = 38f
            typeface = banglaFont
            isAntiAlias = true
        }

        val transliterationPaint = TextPaint().apply {
            color = Color.parseColor("#D0D0D0")
            textSize = 38f
            typeface = banglaFont
            isAntiAlias = true
        }

        val contextPaint = TextPaint().apply {
            color = Color.parseColor("#C0C0C0")
            textSize = 36f
            typeface = banglaFont
            isAntiAlias = true
        }

        val refPaint = TextPaint().apply {
            color = Color.parseColor("#909090")
            textSize = 32f
            typeface = banglaFont
            isAntiAlias = true
        }

        dua.segments.forEachIndexed { sIndex, segment ->
            if (sIndex > 0) {
                currentY += 40f // gap between segments
            }

            // Draw Arabic Text
            if (segment.arabic.isNotEmpty() && segment.arabic != "null") {
                val arabicLayout = StaticLayout.Builder.obtain(segment.arabic, 0, segment.arabic.length, arabicPaint, contentWidth)
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
                currentY += arabicLayout.height + 30f
            }

            // Draw Translation
            if (segment.translation.isNotEmpty() && segment.translation != "null") {
                // Label "অর্থ:"
                if (canvas != null) {
                    canvas.drawText("অর্থ:", margin.toFloat(), currentY + 30f, labelPaint)
                }
                
                // Build StaticLayout for translation text
                val textLayout = StaticLayout.Builder.obtain(segment.translation, 0, segment.translation.length, translationPaint, contentWidth - 40)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(true)
                    .build()

                if (canvas != null) {
                    // Draw dynamic teal indicator bar on the left
                    val indicatorPaint = Paint().apply {
                        color = Color.parseColor("#00E5FF")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawRoundRect(
                        RectF(margin.toFloat(), currentY + 50f, margin.toFloat() + 8f, currentY + 50f + textLayout.height),
                        4f, 4f, indicatorPaint
                    )
                    
                    canvas.save()
                    canvas.translate(margin.toFloat() + 30f, currentY + 50f)
                    textLayout.draw(canvas)
                    canvas.restore()
                }
                currentY += 50f + textLayout.height + 30f
            }

            // Draw Transliteration
            if (segment.transliteration.isNotEmpty() && segment.transliteration != "null") {
                // Label "উচ্চারণ:"
                if (canvas != null) {
                    canvas.drawText("উচ্চারণ:", margin.toFloat(), currentY + 30f, labelPaint)
                }

                val textLayout = StaticLayout.Builder.obtain(segment.transliteration, 0, segment.transliteration.length, transliterationPaint, contentWidth - 40)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(true)
                    .build()

                if (canvas != null) {
                    val indicatorPaint = Paint().apply {
                        color = Color.parseColor("#9600E5FF") // 150/255 alpha (96 in hex)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawRoundRect(
                        RectF(margin.toFloat(), currentY + 50f, margin.toFloat() + 8f, currentY + 50f + textLayout.height),
                        4f, 4f, indicatorPaint
                    )

                    canvas.save()
                    canvas.translate(margin.toFloat() + 30f, currentY + 50f)
                    textLayout.draw(canvas)
                    canvas.restore()
                }
                currentY += 50f + textLayout.height + 30f
            }

            // Draw Prekkhapot (Context)
            if (segment.bottom.isNotEmpty() && segment.bottom != "null") {
                val trimmed = segment.bottom.trim()
                val contextText = if (trimmed.startsWith("দোয়ার প্রেক্ষাপট") || trimmed.startsWith("দোয়ার প্রেক্ষাপট")) {
                    trimmed
                } else {
                    "দোয়ার প্রেক্ষাপট: ${segment.bottom}"
                }

                val textLayout = StaticLayout.Builder.obtain(contextText, 0, contextText.length, contextPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(true)
                    .build()

                if (canvas != null) {
                    canvas.save()
                    canvas.translate(margin.toFloat(), currentY)
                    textLayout.draw(canvas)
                    canvas.restore()
                }
                currentY += textLayout.height + 25f
            }

            // Draw Reference
            if (segment.reference.isNotEmpty() && segment.reference != "null") {
                val textLayout = StaticLayout.Builder.obtain(segment.reference, 0, segment.reference.length, refPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(true)
                    .build()

                if (canvas != null) {
                    canvas.save()
                    canvas.translate(margin.toFloat(), currentY)
                    textLayout.draw(canvas)
                    canvas.restore()
                }
                currentY += textLayout.height + 25f
            }
        }

        currentY += 40f // space before footer divider

        // 5. Draw divider line before footer
        if (canvas != null) {
            val linePaint = Paint().apply {
                color = Color.parseColor("#15FFFFFF") // Faint white line
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(margin.toFloat(), currentY, (width - margin).toFloat(), currentY, linePaint)
        }
        currentY += 40f // gap after divider

        // 6. Draw Footer Credit (App Credit as requested)
        val appCreditText = "❝কুরআন রিডার❞ অ্যাপ থেকে শেয়ারকৃত"
        val creditPaint = TextPaint().apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 34f
            typeface = banglaBoldFont
            isAntiAlias = true
        }

        if (canvas != null) {
            val logoSize = 48
            val textWidth = creditPaint.measureText(appCreditText)
            val totalWidth = logoSize + 16 + textWidth
            val startX = (width - totalWidth) / 2f
            
            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher)
            } catch (e: Exception) {
                null
            }
            
            if (logoBitmap != null) {
                val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)
                canvas.drawBitmap(scaledLogo, startX, currentY + 10f, null)
                canvas.drawText(appCreditText, startX + logoSize + 16f, currentY + 45f, creditPaint)
            } else {
                creditPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(appCreditText, width / 2f, currentY + 45f, creditPaint)
            }
        }
        currentY += 120f // Bottom padding

        return currentY.toInt()
    }

    fun shareAsImage(context: Context, dua: DuaItem) {
        try {
            val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            val banglaNumber = dua.id.toString().map { char ->
                if (char.isDigit()) banglaDigits[char - '0'] else char
            }.joinToString("")

            val arabicText = dua.segments.mapNotNull { it.arabic.takeIf { a -> a.isNotBlank() && a != "null" } }.joinToString("\n\n")
            val transliterationText = dua.segments.mapNotNull { it.transliteration.takeIf { t -> t.isNotBlank() && t != "null" } }.joinToString("\n")
            val translationText = dua.segments.mapNotNull { it.translation.takeIf { tr -> tr.isNotBlank() && tr != "null" } }.joinToString("\n")
                .ifEmpty { dua.title }
            val referenceText = dua.segments.mapNotNull { it.reference.takeIf { r -> r.isNotBlank() && r != "null" } }.firstOrNull()
                ?: "দোয়া নং $banglaNumber"

            val shareData = IslamicCardTemplate.ShareData(
                badgeTitle = "আজকের দোয়া",
                arabicText = arabicText.ifBlank { null },
                transliterationText = transliterationText.ifBlank { null },
                translationText = translationText,
                referenceText = referenceText
            )

            val bitmap = IslamicCardTemplate.generateCardBitmap(context, shareData)
            
            val cacheDir = File(context.cacheDir, "shared_images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, "dua_${dua.id}.png")
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
            context.startActivity(Intent.createChooser(intent, "দোয়া শেয়ার করুন (ছবি)"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "ছবি তৈরি করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
