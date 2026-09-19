package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.PrayerName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object PrayerTimesShareUtil {

    private val fontCache = java.util.concurrent.ConcurrentHashMap<Int, Typeface>()

    private fun getFont(context: Context, resId: Int): Typeface {
        return fontCache.getOrPut(resId) {
            try {
                ResourcesCompat.getFont(context, resId) ?: Typeface.DEFAULT
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
    }

    fun buildShareText(
        schedule: DailyPrayerSchedule,
        date: LocalDate = LocalDate.now(),
        hijriOffset: Int = 0
    ): String {
        val hijriInfo = HijriCalendarUtil.getHijriDate(date, hijriOffset)
        val hijriStr = "${DateUtil.toBengaliNumerals(hijriInfo.hijriDay)} ${hijriInfo.hijriMonthNameBn}, ${DateUtil.toBengaliNumerals(hijriInfo.hijriYear)} হিজরী"
        val banglaStr = HijriCalendarUtil.getBanglaDateStr(date, includeSuffix = true)
        val weekdayStr = HijriCalendarUtil.getBengaliWeekdayName(date)

        val fajrItem = schedule.prayers.find { it.name == PrayerName.FAJR }
        val dhuhrItem = schedule.prayers.find { it.name == PrayerName.DHUHR }
        val asrItem = schedule.prayers.find { it.name == PrayerName.ASR }
        val maghribItem = schedule.prayers.find { it.name == PrayerName.MAGHRIB }
        val ishaItem = schedule.prayers.find { it.name == PrayerName.ISHA }

        val sunrise = schedule.sunriseTimeDigits.ifEmpty { schedule.forbiddenSunriseFormatted }
        val sunset = schedule.sunsetTimeDigits.ifEmpty { schedule.forbiddenSunsetFormatted }

        return buildString {
            append("🕌 দৈনিক নামাজের সময়সূচি 🕌\n")
            append("📍 স্থান: ${schedule.district.nameBn} জেলা\n")
            append("─────────────────\n")
            append("📅 ইংরেজি: ${schedule.dateStrBn} ($weekdayStr)\n")
            append("🌙 হিজরী: $hijriStr\n")
            append("🌾 বাংলা: $banglaStr\n")
            append("─────────────────\n")
            append("【 ওয়াক্তের সময়সূচি 】\n")
            append("🌅 ফজর: শুরু ${fajrItem?.timeDigits ?: "০৪:১০"} - শেষ $sunrise\n")
            append("☀️ যুহর: শুরু ${dhuhrItem?.timeDigits ?: "১২:০৪"} - শেষ ${asrItem?.timeDigits ?: "০৪:৩৭"}\n")
            append("🌤️ আসর: শুরু ${asrItem?.timeDigits ?: "০৪:৩৮"} - শেষ ${maghribItem?.timeDigits ?: "০৬:৩১"}\n")
            append("🌇 মাগরিব: শুরু ${maghribItem?.timeDigits ?: "০৬:৩২"} - শেষ ${ishaItem?.timeDigits ?: "০৭:৫১"}\n")
            append("🌙 ইশা: শুরু ${ishaItem?.timeDigits ?: "০৭:৫২"} - শেষ ${fajrItem?.timeDigits ?: "০৪:১০"}\n")
            append("─────────────────\n")
            append("🌙 সাহরি শেষ: ${schedule.sahriTimeDigits.ifEmpty { schedule.sahriEndTimeFormatted }}\n")
            append("🌇 ইফতার শুরু: ${schedule.iftarTimeDigits.ifEmpty { schedule.iftarTimeFormatted }}\n")
            append("☀️ সূর্যোদয়: $sunrise\n")
            append("🌅 সূর্যাস্ত: $sunset\n")
            append("✨ দুহা / ইশরাক: ${schedule.ishraqStartTimeFormatted}\n")
            append("🌌 তাহাজ্জুদ শেষ: ${schedule.tahajjudEndTimeFormatted}\n")
            append("─────────────────\n")
            append("⚠️ নামাজের ৩টি নিষিদ্ধ সময় (মাকরূহ):\n")
            val forbiddenItems = schedule.forbiddenTimesList.ifEmpty {
                listOf(
                    com.example.data.model.ForbiddenPrayerInterval("সূর্যোদয়ের সময়", schedule.forbiddenSunriseFormatted, "সূর্য ওঠা থেকে ১৫ মিনিট পর্যন্ত"),
                    com.example.data.model.ForbiddenPrayerInterval("দ্বিপ্রহরের সময় (জাওয়াল)", schedule.forbiddenMiddayFormatted, "ঠিক দুপুরে সূর্য মাথার ওপর থাকাকালে"),
                    com.example.data.model.ForbiddenPrayerInterval("সূর্যাস্তের সময়", schedule.forbiddenSunsetFormatted, "সূর্যাস্তের পূর্ববর্তী ১৫ মিনিট থেকে মাগরিব পর্যন্ত")
                )
            }
            forbiddenItems.forEachIndexed { idx, forbidden ->
                append("${idx + 1}. ${forbidden.titleBn}: ${forbidden.timeRangeBn}\n")
            }
            append("─────────────────\n")
            append("📌 সূত্র: ইসলামিক ফাউন্ডেশন (বাংলাদেশ)\n")
            append("📘 Facebook: fb.com/MuslimsLibrary\n")
            append("✈️ Telegram: t.me/MuslimsLibraryApp\n")
            append("📱 Quran Reader App")
        }
    }

    fun copyToClipboard(
        context: Context,
        schedule: DailyPrayerSchedule,
        date: LocalDate = LocalDate.now(),
        hijriOffset: Int = 0
    ) {
        val text = buildShareText(schedule, date, hijriOffset)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Prayer Times", text)
        clipboard.setPrimaryClip(clip)
    }

    fun generatePrayerCardBitmap(
        context: Context,
        schedule: DailyPrayerSchedule,
        date: LocalDate = LocalDate.now(),
        hijriOffset: Int = 0
    ): Bitmap {
        val width = 1080
        val height = 1240
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val boldFont = getFont(context, R.font.solaimanlipi_bold)
        val regularFont = getFont(context, R.font.solaimanlipi)

        // Date strings calculation
        val hijriInfo = HijriCalendarUtil.getHijriDate(date, hijriOffset)
        val hijriDateStr = "${DateUtil.toBengaliNumerals(hijriInfo.hijriDay)} ${hijriInfo.hijriMonthNameBn}, ${DateUtil.toBengaliNumerals(hijriInfo.hijriYear)}"
        val banglaDateStr = HijriCalendarUtil.getBanglaDateStr(date, includeSuffix = true)
        val weekdayStr = HijriCalendarUtil.getBengaliWeekdayName(date)
        val engDateStr = schedule.dateStrBn

        // 1. Clean Pastel Mint-Green Background Gradient
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#F3FAF6"),
                    Color.parseColor("#EBF5F0"),
                    Color.parseColor("#DFEFE7")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Artistic Dark Green Waves & Curves on Right and Bottom Edges
        val wavePaint1 = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#17634B")
            style = Paint.Style.FILL
        }
        val wavePaint2 = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#0F4633")
            style = Paint.Style.FILL
        }

        // Deep wave right-bottom
        val path1 = Path().apply {
            moveTo(width * 0.45f, height.toFloat())
            cubicTo(
                width * 0.68f, height * 0.90f,
                width * 0.82f, height * 0.65f,
                width.toFloat(), height * 0.44f
            )
            lineTo(width.toFloat(), height.toFloat())
            close()
        }
        canvas.drawPath(path1, wavePaint1)

        val path2 = Path().apply {
            moveTo(width * 0.60f, height.toFloat())
            cubicTo(
                width * 0.78f, height * 0.94f,
                width * 0.88f, height * 0.76f,
                width.toFloat(), height * 0.54f
            )
            lineTo(width.toFloat(), height.toFloat())
            close()
        }
        canvas.drawPath(path2, wavePaint2)

        // Mosque Silhouette Layer across the bottom right curve
        val mosquePaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(35, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val mosquePath = Path().apply {
            val baseY = height.toFloat()
            moveTo(width * 0.56f, baseY)
            lineTo(width * 0.58f, baseY - 45f)
            lineTo(width * 0.60f, baseY - 45f)
            lineTo(width * 0.60f, baseY - 90f)
            lineTo(width * 0.61f, baseY - 105f)
            lineTo(width * 0.62f, baseY - 90f)
            lineTo(width * 0.62f, baseY - 45f)
            lineTo(width * 0.65f, baseY - 45f)
            // Dome 1
            val d1cx = width * 0.70f
            cubicTo(d1cx - 28f, baseY - 45f, d1cx - 22f, baseY - 100f, d1cx, baseY - 110f)
            cubicTo(d1cx + 22f, baseY - 100f, d1cx + 28f, baseY - 45f, width * 0.75f, baseY - 45f)
            // Minaret
            lineTo(width * 0.78f, baseY - 45f)
            lineTo(width * 0.78f, baseY - 120f)
            lineTo(width * 0.79f, baseY - 136f)
            lineTo(width * 0.80f, baseY - 120f)
            lineTo(width * 0.80f, baseY - 45f)
            // Dome 2
            val d2cx = width * 0.86f
            cubicTo(d2cx - 28f, baseY - 45f, d2cx - 22f, baseY - 92f, d2cx, baseY - 102f)
            cubicTo(d2cx + 22f, baseY - 92f, d2cx + 28f, baseY - 45f, width * 0.91f, baseY - 45f)
            lineTo(width.toFloat(), baseY - 45f)
            lineTo(width.toFloat(), baseY)
            close()
        }
        canvas.drawPath(mosquePath, mosquePaint)

        // 3. Top-Right: App Launcher Icon + App Name "Quran Reader"
        val appIconSize = 54f
        val iconRight = width - 68f
        val iconTop = 26f
        val iconRect = RectF(iconRight - appIconSize, iconTop, iconRight, iconTop + appIconSize)

        try {
            val launcherBmp = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher)
                ?: BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            if (launcherBmp != null) {
                val iconClipPath = Path().apply {
                    addRoundRect(iconRect, 14f, 14f, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(iconClipPath)
                canvas.drawBitmap(launcherBmp, null, iconRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            } else {
                val iconBgPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#15803D")
                }
                canvas.drawRoundRect(iconRect, 14f, 14f, iconBgPaint)
            }
        } catch (e: Exception) {
            val iconBgPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#15803D")
            }
            canvas.drawRoundRect(iconRect, 14f, 14f, iconBgPaint)
        }

        val appNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
            typeface = boldFont
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Quran Reader", iconRight, iconTop + appIconSize + 24f, appNamePaint)

        // 4. Main Floating White Card in Center
        val cardLeft = 66f
        val cardTop = 114f
        val cardRight = width - 66f
        val cardBottom = height - 88f
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // Card Shadow & Body
        val cardShadowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(22, 0, 0, 0)
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(RectF(cardLeft, cardTop + 6f, cardRight, cardBottom + 6f), 30f, 30f, cardShadowPaint)

        val mainCardBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(252, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val mainCardBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#BFE2D5")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        canvas.drawRoundRect(cardRect, 30f, 30f, mainCardBgPaint)
        canvas.drawRoundRect(cardRect, 30f, 30f, mainCardBorderPaint)

        // 5. 3D Golden Crescent Moon at Top-Left of Floating Card
        val moonCx = cardLeft + 24f
        val moonCy = cardTop - 12f
        val moonRadius = 34f

        val moonPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                moonCx - 8f, moonCy - 8f, moonRadius * 1.5f,
                intArrayOf(
                    Color.parseColor("#FDE047"),
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#D97706")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val moonOuterPath = Path().apply {
            addCircle(moonCx, moonCy, moonRadius, Path.Direction.CW)
        }
        val moonInnerPath = Path().apply {
            addCircle(moonCx + 13f, moonCy - 7f, moonRadius * 0.88f, Path.Direction.CW)
        }
        moonOuterPath.op(moonInnerPath, Path.Op.DIFFERENCE)
        canvas.drawPath(moonOuterPath, moonPaint)

        // 6. Header Inside Card: Weekday, 3 Dates & Location
        val weekdayPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#114D38")
            textSize = 44f
            typeface = boldFont
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(weekdayStr, cardLeft + 30f, cardTop + 52f, weekdayPaint)

        val datesSubtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 20.5f
            typeface = regularFont
            textAlign = Paint.Align.LEFT
        }
        val datesCombinedStr = "$engDateStr  •  $hijriDateStr হিজরী  •  $banglaDateStr"
        canvas.drawText(datesCombinedStr, cardLeft + 30f, cardTop + 86f, datesSubtitlePaint)

        // Location Right Side (Pin + District)
        val locPinPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("📍", cardRight - 195f, cardTop + 62f, locPinPaint)

        val locTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = boldFont
            textAlign = Paint.Align.LEFT
        }
        val locSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 18.5f
            typeface = regularFont
            textAlign = Paint.Align.LEFT
        }
        val districtName = schedule.district.nameEn.ifEmpty { schedule.district.nameBn }
        val countryName = schedule.district.countryEn.ifEmpty { schedule.district.countryBn }
        canvas.drawText(districtName, cardRight - 185f, cardTop + 52f, locTitlePaint)
        canvas.drawText(countryName, cardRight - 185f, cardTop + 80f, locSubPaint)

        // 7. Table Setup Definitions
        val tableMargin = 26f
        val tableLeft = cardLeft + tableMargin
        val tableRight = cardRight - tableMargin
        val tableWidth = tableRight - tableLeft

        val vCol1 = tableLeft + (tableWidth * 0.44f)
        val vCol2 = tableLeft + (tableWidth * 0.72f)

        val col1X = tableLeft + 24f
        val col2X = (vCol1 + vCol2) / 2f
        val col3X = (vCol2 + tableRight) / 2f

        // Clear, high-contrast crisp grid lines & table border paints
        val tableBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#8ECCAE")
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
        }
        val tableInnerGridPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#A8D8C0")
            strokeWidth = 1.6f
        }
        val rowBgEven = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val rowNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 23.5f
            typeface = boldFont
            textAlign = Paint.Align.LEFT
        }
        val rowTimePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 24.5f
            typeface = boldFont
            textAlign = Paint.Align.CENTER
        }

        var currentY = cardTop + 108f

        // ==========================================
        // 8. TABLE GROUP 1: ওয়াক্তের সময়সূচি (7 Rows)
        // ==========================================
        val thHeight = 42f
        val t1RowHeight = 42f
        val fajrItem = schedule.prayers.find { it.name == PrayerName.FAJR }
        val dhuhrItem = schedule.prayers.find { it.name == PrayerName.DHUHR }
        val asrItem = schedule.prayers.find { it.name == PrayerName.ASR }
        val maghribItem = schedule.prayers.find { it.name == PrayerName.MAGHRIB }
        val ishaItem = schedule.prayers.find { it.name == PrayerName.ISHA }

        val sunrise = schedule.sunriseTimeDigits.ifEmpty { "০৫:৩১" }
        val sunset = schedule.sunsetTimeDigits.ifEmpty { "০৬:৩২" }
        val sahri = schedule.sahriTimeDigits.ifEmpty { "০৪:১০" }
        val iftar = schedule.iftarTimeDigits.ifEmpty { "০৬:৩২" }

        // Duha time clean extraction (without 'সকাল' / 'AM' / 'PM')
        fun cleanTimeDigits(raw: String): String {
            return raw.replace(" AM", "")
                .replace(" PM", "")
                .replace("সকাল", "")
                .replace("দুপুর", "")
                .replace("বিকাল", "")
                .replace("সন্ধ্যা", "")
                .replace("রাত", "")
                .trim()
        }

        val duhaParts = schedule.duhaRange.split("-")
        val duhaStart = if (duhaParts.isNotEmpty() && duhaParts[0].isNotBlank()) {
            cleanTimeDigits(duhaParts[0])
        } else {
            cleanTimeDigits(schedule.ishraqStartTimeFormatted)
        }
        val duhaEnd = if (duhaParts.size > 1 && duhaParts[1].isNotBlank()) {
            cleanTimeDigits(duhaParts[1])
        } else {
            cleanTimeDigits(schedule.zawalStartTime.ifEmpty { "১১:৫৬" })
        }

        data class TableRowItem(val name: String, val start: String, val end: String)
        val t1Rows = listOf(
            TableRowItem("ফজর", fajrItem?.timeDigits ?: "০৪:১০", sunrise),
            TableRowItem("যুহর", dhuhrItem?.timeDigits ?: "১২:০৪", asrItem?.timeDigits ?: "০৪:৩৭"),
            TableRowItem("আসর", asrItem?.timeDigits ?: "০৪:৩৮", maghribItem?.timeDigits ?: "০৬:৩১"),
            TableRowItem("মাগরিব", maghribItem?.timeDigits ?: "০৬:৩২", ishaItem?.timeDigits ?: "০৭:৫১"),
            TableRowItem("ইশা", ishaItem?.timeDigits ?: "০৭:৫২", fajrItem?.timeDigits ?: "০৪:১০"),
            TableRowItem("দুহা", duhaStart, duhaEnd),
            TableRowItem("তাহাজ্জুদ", ishaItem?.timeDigits ?: "০৭:৫২", sahri)
        )

        val t1TotalH = thHeight + t1Rows.size * t1RowHeight
        val t1BoxRect = RectF(tableLeft, currentY, tableRight, currentY + t1TotalH)

        // Draw Table 1 Container
        canvas.drawRoundRect(t1BoxRect, 14f, 14f, rowBgEven)

        // Draw Header
        val thRect = RectF(tableLeft, currentY, tableRight, currentY + thHeight)
        val thBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#C9EBDC")
            style = Paint.Style.FILL
        }
        val thClipPath = Path().apply {
            addRoundRect(
                thRect,
                floatArrayOf(14f, 14f, 14f, 14f, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(thClipPath)
        canvas.drawRect(thRect, thBgPaint)
        canvas.restore()

        val thTextLeft = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#114D38")
            textSize = 23.5f
            typeface = boldFont
            textAlign = Paint.Align.LEFT
        }
        val thTextCol = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#114D38")
            textSize = 23.5f
            typeface = boldFont
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ওয়াক্ত", col1X, currentY + 29f, thTextLeft)
        canvas.drawText("শুরু", col2X, currentY + 29f, thTextCol)
        canvas.drawText("শেষ", col3X, currentY + 29f, thTextCol)

        // Draw Rows
        var rowTopY = currentY + thHeight
        t1Rows.forEachIndexed { index, row ->
            val centerY = rowTopY + 29f
            canvas.drawText(row.name, col1X, centerY, rowNamePaint)
            canvas.drawText(row.start, col2X, centerY, rowTimePaint)
            canvas.drawText(row.end, col3X, centerY, rowTimePaint)

            // Horizontal divider line
            val lineY = rowTopY + t1RowHeight
            if (index < t1Rows.size - 1) {
                canvas.drawLine(tableLeft, lineY, tableRight, lineY, tableInnerGridPaint)
            }
            rowTopY += t1RowHeight
        }

        // Header bottom divider line
        canvas.drawLine(tableLeft, currentY + thHeight, tableRight, currentY + thHeight, tableInnerGridPaint)

        // Vertical dividers for Table 1
        canvas.drawLine(vCol1, currentY, vCol1, currentY + t1TotalH, tableInnerGridPaint)
        canvas.drawLine(vCol2, currentY, vCol2, currentY + t1TotalH, tableInnerGridPaint)

        // Outer border for Table 1
        canvas.drawRoundRect(t1BoxRect, 14f, 14f, tableBorderPaint)

        currentY += t1TotalH + 16f

        // =========================================================================
        // 9. TABLE GROUP 2: সাহরি, ইফতার, সূর্যোদয় ও সূর্যাস্ত (Separate Table Box)
        // =========================================================================
        val t2RowHeight = 41f
        val t2Rows = listOf(
            Pair("সাহরি শেষ", sahri),
            Pair("ইফতার শুরু", iftar),
            Pair("সূর্যোদয়", sunrise),
            Pair("সূর্যাস্ত", sunset)
        )
        val t2TotalH = t2Rows.size * t2RowHeight
        val t2BoxRect = RectF(tableLeft, currentY, tableRight, currentY + t2TotalH)

        // Draw Table 2 Container
        canvas.drawRoundRect(t2BoxRect, 14f, 14f, rowBgEven)

        val rightColCenter = (vCol1 + tableRight) / 2f
        var t2RowTopY = currentY
        t2Rows.forEachIndexed { index, item ->
            val centerY = t2RowTopY + 28f
            canvas.drawText(item.first, col1X, centerY, rowNamePaint)
            canvas.drawText(item.second, rightColCenter, centerY, rowTimePaint)

            val lineY = t2RowTopY + t2RowHeight
            if (index < t2Rows.size - 1) {
                canvas.drawLine(tableLeft, lineY, tableRight, lineY, tableInnerGridPaint)
            }
            t2RowTopY += t2RowHeight
        }

        // Vertical divider for Table 2
        canvas.drawLine(vCol1, currentY, vCol1, currentY + t2TotalH, tableInnerGridPaint)

        // Outer border for Table 2
        canvas.drawRoundRect(t2BoxRect, 14f, 14f, tableBorderPaint)

        currentY += t2TotalH + 16f

        // ==================================================================================
        // 10. TABLE GROUP 3: ৩টি নিষিদ্ধ সময় (মাকরূহ) (Separate Table Box)
        // ==================================================================================
        val t3HeaderHeight = 38f
        val t3RowHeight = 40f

        val fMorningStr = schedule.forbiddenMorningRange.ifEmpty {
            cleanTimeDigits(schedule.forbiddenSunriseFormatted)
        }
        val fNoonStr = schedule.forbiddenNoonRange.ifEmpty {
            cleanTimeDigits(schedule.forbiddenMiddayFormatted)
        }
        val fEveStr = schedule.forbiddenEveningRange.ifEmpty {
            cleanTimeDigits(schedule.forbiddenSunsetFormatted)
        }

        val t3Rows = listOf(
            Pair("১. সূর্যোদয়ের সময়", fMorningStr),
            Pair("২. দ্বিপ্রহরের সময় (জাওয়াল)", fNoonStr),
            Pair("৩. সূর্যাস্তের সময়", fEveStr)
        )
        val t3TotalH = t3HeaderHeight + t3Rows.size * t3RowHeight
        val t3BoxRect = RectF(tableLeft, currentY, tableRight, currentY + t3TotalH)

        val forbiddenTableBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FCA5A5")
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
        }
        val forbiddenGridPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FECACA")
            strokeWidth = 1.6f
        }
        val forbiddenBg = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFFBFB")
            style = Paint.Style.FILL
        }

        // Draw Table 3 Container
        canvas.drawRoundRect(t3BoxRect, 14f, 14f, forbiddenBg)

        // Draw Header
        val t3ThRect = RectF(tableLeft, currentY, tableRight, currentY + t3HeaderHeight)
        val t3ThBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FEE2E2")
            style = Paint.Style.FILL
        }
        val t3ClipPath = Path().apply {
            addRoundRect(
                t3ThRect,
                floatArrayOf(14f, 14f, 14f, 14f, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(t3ClipPath)
        canvas.drawRect(t3ThRect, t3ThBgPaint)
        canvas.restore()

        val fTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B91C1C")
            textSize = 21f
            typeface = boldFont
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("⚠️ নামাজের ৩টি নিষিদ্ধ সময় (মাকরূহ):", tableLeft + 18f, currentY + 27f, fTitlePaint)

        // Header bottom divider line
        canvas.drawLine(tableLeft, currentY + t3HeaderHeight, tableRight, currentY + t3HeaderHeight, forbiddenGridPaint)

        val fRowNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7F1D1D")
            textSize = 22f
            typeface = boldFont
            textAlign = Paint.Align.LEFT
        }
        val fRowTimePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#991B1B")
            textSize = 23f
            typeface = boldFont
            textAlign = Paint.Align.CENTER
        }

        var t3RowTopY = currentY + t3HeaderHeight
        t3Rows.forEachIndexed { index, item ->
            val centerY = t3RowTopY + 28f
            canvas.drawText(item.first, col1X, centerY, fRowNamePaint)
            // Perfectly centered within the right column
            canvas.drawText(item.second, rightColCenter, centerY, fRowTimePaint)

            val lineY = t3RowTopY + t3RowHeight
            if (index < t3Rows.size - 1) {
                canvas.drawLine(tableLeft, lineY, tableRight, lineY, forbiddenGridPaint)
            }
            t3RowTopY += t3RowHeight
        }

        // Vertical divider for Table 3
        canvas.drawLine(vCol1, currentY + t3HeaderHeight, vCol1, currentY + t3TotalH, forbiddenGridPaint)

        // Outer border for Table 3
        canvas.drawRoundRect(t3BoxRect, 14f, 14f, forbiddenTableBorderPaint)

        // =========================================================================
        // 11. Footer Credit & Social Links (Original Facebook & Telegram Icons)
        // =========================================================================
        val footerY = height - 42f

        // App Name on Left
        val footerAppPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 25f
            typeface = boldFont
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Quran Reader", 64f, footerY - 4f, footerAppPaint)

        // 12. DRAW OFFICIAL FACEBOOK ICON + TEXT
        val fbX = 265f
        val fbY = footerY - 12f
        val fbRadius = 15f

        val fbBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1877F2")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(fbX, fbY, fbRadius, fbBgPaint)

        val fbLetterPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val fbPath = Path().apply {
            moveTo(fbX + 3.5f, fbY + 10f)
            lineTo(fbX - 0.8f, fbY + 10f)
            lineTo(fbX - 0.8f, fbY + 1f)
            lineTo(fbX - 4f, fbY + 1f)
            lineTo(fbX - 4f, fbY - 3f)
            lineTo(fbX - 0.8f, fbY - 3f)
            lineTo(fbX - 0.8f, fbY - 6.5f)
            cubicTo(fbX - 0.8f, fbY - 10.5f, fbX + 1.5f, fbY - 11.5f, fbX + 5f, fbY - 11.5f)
            lineTo(fbX + 5f, fbY - 8f)
            lineTo(fbX + 3f, fbY - 8f)
            cubicTo(fbX + 1.8f, fbY - 8f, fbX + 1.8f, fbY - 7f, fbX + 1.8f, fbY - 5.5f)
            lineTo(fbX + 1.8f, fbY - 3f)
            lineTo(fbX + 5f, fbY - 3f)
            lineTo(fbX + 4.3f, fbY + 1f)
            lineTo(fbX + 1.8f, fbY + 1f)
            lineTo(fbX + 1.8f, fbY + 10f)
            close()
        }
        canvas.drawPath(fbPath, fbLetterPaint)

        val socialTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 21f
            typeface = boldFont
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("/MuslimsLibrary", fbX + fbRadius + 7f, footerY - 5f, socialTextPaint)

        // 13. DRAW OFFICIAL TELEGRAM ICON + TEXT
        val tgX = 515f
        val tgY = footerY - 12f
        val tgRadius = 15f

        val tgBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#2AABEE")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(tgX, tgY, tgRadius, tgBgPaint)

        val tgPaperPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val tgPath = Path().apply {
            moveTo(tgX - 7.5f, tgY - 1f)
            lineTo(tgX + 8f, tgY - 6.5f)
            lineTo(tgX + 3.5f, tgY + 6.5f)
            lineTo(tgX, tgY + 2f)
            lineTo(tgX - 3.2f, tgY + 4f)
            lineTo(tgX - 3.2f, tgY + 0.5f)
            lineTo(tgX + 5f, tgY - 3f)
            lineTo(tgX - 4.5f, tgY + 0.5f)
            close()
        }
        canvas.drawPath(tgPath, tgPaperPaint)

        canvas.drawText("/MuslimsLibraryApp", tgX + tgRadius + 7f, footerY - 5f, socialTextPaint)

        return bitmap
    }

    suspend fun shareAsImage(
        context: Context,
        schedule: DailyPrayerSchedule,
        date: LocalDate = LocalDate.now(),
        hijriOffset: Int = 0
    ) {
        try {
            val bitmap = withContext(Dispatchers.Default) {
                generatePrayerCardBitmap(context, schedule, date, hijriOffset)
            }

            val cacheDir = File(context.cacheDir, "shared_prayer_cards")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, "prayer_schedule_${System.currentTimeMillis()}.png")
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, buildShareText(schedule, date, hijriOffset))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "নামাজের সময়সূচি ফটো কার্ড শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ফটো কার্ড শেয়ার করতে ব্যর্থ হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
