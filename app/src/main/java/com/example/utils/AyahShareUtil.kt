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
import com.example.data.model.CombinedAyah
import java.io.File
import java.io.FileOutputStream

object AyahShareUtil {

    fun buildAyahShareText(ayah: CombinedAyah, surahName: String): String {
        return buildString {
            append("📖 আল-কুরআন • ").append(surahName).append(" • আয়াত ").append(ayah.numberInSurah).append("\n\n")
            append("আরবি:\n").append(ayah.arabicText).append("\n\n")
            append("অর্থ:\n").append(ayah.bengaliText).append("\n")
            
            if (!ayah.tafsirText.isNullOrEmpty()) {
                append("\nতাফসীর:\n")
                val cleanTafsir = try {
                    android.text.Html.fromHtml(ayah.tafsirText, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                } catch (e: Exception) {
                    ayah.tafsirText
                }
                append(cleanTafsir).append("\n")
            }
            
            append("\n---\n")
            append("📱 ❝কুরআন রিডার❞ অ্যাপ থেকে শেয়ারকৃত।")
        }
    }

    fun copyToClipboard(context: Context, ayah: CombinedAyah, surahName: String) {
        val shareText = buildAyahShareText(ayah, surahName)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Ayah Text", shareText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "আয়াত ও তাফসীর ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
    }

    fun shareAsText(context: Context, ayah: CombinedAyah, surahName: String) {
        val shareText = buildAyahShareText(ayah, surahName)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "আয়াত শেয়ার করুন (টেক্সট)"))
    }

    /**
     * Converts an Ayah to a QuickCardPayload for Photo Card Maker.
     */
    fun createPhotoCardPayload(ayah: CombinedAyah, surahName: String): PhotoCardBridge.QuickCardPayload {
        return PhotoCardBridge.fromAyah(ayah, surahName)
    }

    /**
     * Converts an Ayah to a ShortPost for the card creator dialog.
     */
    fun toShortPost(ayah: CombinedAyah, surahName: String): com.example.data.model.ShortPost {
        return PhotoCardBridge.fromAyah(ayah, surahName).toShortPost()
    }

    private fun measureAndDrawAyah(canvas: Canvas?, ayah: CombinedAyah, surahName: String, context: Context): Int {
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

        // 1. Draw top category pill
        val categoryText = "$surahName • আয়াত ${ayah.numberInSurah}"
        val categoryPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = banglaBoldFont
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        if (canvas != null) {
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
        currentY += 100f

        // 2. Arabic Text
        val arabicPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 64f
            typeface = arabicFont
            isAntiAlias = true
        }
        val arabicLayout = StaticLayout.Builder.obtain(ayah.arabicText, 0, ayah.arabicText.length, arabicPaint, contentWidth)
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

        // 3. Divider line
        if (canvas != null) {
            val linePaint = Paint().apply {
                color = Color.parseColor("#20FFFFFF")
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(margin.toFloat(), currentY, (width - margin).toFloat(), currentY, linePaint)
        }
        currentY += 50f

        // 4. Bengali Translation
        val translationLabelPaint = TextPaint().apply {
            color = Color.parseColor("#00E5FF") // High-contrast cyan
            textSize = 34f
            typeface = banglaBoldFont
            isAntiAlias = true
        }
        if (canvas != null) {
            canvas.drawText("অর্থ:", margin.toFloat(), currentY + 30f, translationLabelPaint)
        }
        currentY += 50f

        val translationPaint = TextPaint().apply {
            color = Color.parseColor("#EAEAEA")
            textSize = 38f
            typeface = banglaFont
            isAntiAlias = true
        }
        val translationLayout = StaticLayout.Builder.obtain(ayah.bengaliText, 0, ayah.bengaliText.length, translationPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)
            .build()

        if (canvas != null) {
            canvas.save()
            canvas.translate(margin.toFloat(), currentY)
            translationLayout.draw(canvas)
            canvas.restore()
        }
        currentY += translationLayout.height + 40f

        // 5. Tafsir rendering section has been removed to keep the shared image layout clean and fully readable.


        // 6. Draw Divider before Footer
        if (canvas != null) {
            val linePaint = Paint().apply {
                color = Color.parseColor("#15FFFFFF")
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(margin.toFloat(), currentY, (width - margin).toFloat(), currentY, linePaint)
        }
        currentY += 40f

        // 7. Draw Footer Credit (App Logo & Name)
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

    fun shareAsImage(context: Context, ayah: CombinedAyah, surahName: String) {
        try {
            val shareData = IslamicCardTemplate.ShareData(
                badgeTitle = "আজকের আয়াত",
                arabicText = ayah.arabicText,
                transliterationText = null,
                translationText = ayah.bengaliText,
                referenceText = "$surahName : ${com.example.utils.DateUtil.toBengaliNumerals(ayah.numberInSurah)}"
            )

            val bitmap = IslamicCardTemplate.generateCardBitmap(context, shareData)
            
            val cacheDir = File(context.cacheDir, "shared_images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, "ayah_${ayah.number}.png")
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
