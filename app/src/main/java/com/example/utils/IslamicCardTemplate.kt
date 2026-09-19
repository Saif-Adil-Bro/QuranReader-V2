package com.example.utils

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import kotlin.math.max

object IslamicCardTemplate {

    data class ShareData(
        val badgeTitle: String,
        val subtitle: String? = null,
        val arabicText: String? = null,
        val transliterationText: String? = null,
        val translationText: String,
        val referenceText: String? = null,
        val footerAppName: String = "Quran READER",
        val footerAppSubtext: String = "কুরআন রিডার অ্যাপ থেকে শেয়ারকৃত"
    )

    private fun drawRealisticLeaf(
        canvas: Canvas,
        baseX: Float,
        baseY: Float,
        angleDegrees: Float,
        length: Float = 60f,
        width: Float = 24f
    ) {
        canvas.save()
        canvas.translate(baseX, baseY)
        canvas.rotate(angleDegrees)

        val leafPath = Path().apply {
            moveTo(0f, 0f)
            cubicTo(length * 0.3f, -width * 0.6f, length * 0.7f, -width * 0.5f, length, 0f)
            cubicTo(length * 0.7f, width * 0.5f, length * 0.3f, width * 0.6f, 0f, 0f)
            close()
        }

        val leafGradient = LinearGradient(
            0f, 0f, length, 0f,
            intArrayOf(
                Color.parseColor("#123D24"),
                Color.parseColor("#2E7D32"),
                Color.parseColor("#52B788")
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )

        val leafPaint = Paint().apply {
            shader = leafGradient
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(leafPath, leafPaint)

        // Center Vein
        val veinPaint = Paint().apply {
            color = Color.parseColor("#D8F3DC")
            strokeWidth = 2.2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(0f, 0f, length * 0.88f, 0f, veinPaint)

        // Side veins
        val sideVeinPaint = Paint().apply {
            color = Color.parseColor("#95D5B2")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val steps = 3
        for (i in 1..steps) {
            val vx = length * (i * 0.22f)
            canvas.drawLine(vx, 0f, vx + 10f, -width * 0.28f, sideVeinPaint)
            canvas.drawLine(vx, 0f, vx + 10f, width * 0.28f, sideVeinPaint)
        }

        canvas.restore()
    }

    private fun drawIslamicPatternOverlay(canvas: Canvas, width: Int, height: Int) {
        val patternPaint = Paint().apply {
            color = Color.parseColor("#14FFE082") // 8% gold tint pattern
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        val step = 140f
        var y = 30f
        while (y < height) {
            var x = 30f
            while (x < width) {
                // Draw 8-point geometric star
                val r = 24f
                val innerR = 12f
                val starPath = Path()
                for (i in 0 until 16) {
                    val radius = if (i % 2 == 0) r else innerR
                    val angle = Math.toRadians((i * 22.5) - 90)
                    val px = (x + radius * Math.cos(angle)).toFloat()
                    val py = (y + radius * Math.sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                canvas.drawPath(starPath, patternPaint)

                // Connecting lines
                canvas.drawCircle(x, y, 4f, patternPaint)
                x += step
            }
            y += step
        }
    }

    private fun drawGoldFloralFlourish(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        canvas.save()
        canvas.translate(cx, cy)
        for (i in 0 until 8) {
            val angle = i * 45f
            canvas.save()
            canvas.rotate(angle)
            val p = Path().apply {
                moveTo(0f, 0f)
                quadTo(size * 0.35f, -size * 0.25f, size, 0f)
                quadTo(size * 0.35f, size * 0.25f, 0f, 0f)
                close()
            }
            canvas.drawPath(p, paint)
            canvas.restore()
        }
        canvas.drawCircle(0f, 0f, size * 0.22f, paint)
        canvas.restore()
    }

    fun generateCardBitmap(context: Context, data: ShareData): Bitmap {
        val width = 1080
        val marginX = 64f
        val cardWidth = width - 2 * marginX // 952px
        val contentPaddingX = 54f
        val contentWidth = (cardWidth - 2 * contentPaddingX).toInt() // 844px

        // Custom fonts
        val arabicFont = try {
            ResourcesCompat.getFont(context, R.font.scheherazade_new)
                ?: ResourcesCompat.getFont(context, R.font.amiri_regular)
                ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val banglaFont = try {
            ResourcesCompat.getFont(context, R.font.solaimanlipi) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val banglaBoldFont = try {
            ResourcesCompat.getFont(context, R.font.solaimanlipi_bold) ?: Typeface.DEFAULT_BOLD
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }

        // 1. Arabic Text Layout (Hero Element)
        val formattedArabic = if (!data.arabicText.isNullOrBlank()) {
            data.arabicText.trim()
        } else ""

        val arabicPaint = TextPaint().apply {
            color = Color.parseColor("#18352B") // Rich dark emerald
            textSize = when {
                formattedArabic.length > 250 -> 48f
                formattedArabic.length > 120 -> 54f
                formattedArabic.length > 60 -> 60f
                else -> 66f
            }
            typeface = arabicFont
            isAntiAlias = true
        }

        val arabicLayout = if (formattedArabic.isNotEmpty()) {
            StaticLayout.Builder.obtain(formattedArabic, 0, formattedArabic.length, arabicPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(true)
                .build()
        } else null

        // 2. Transliteration Layout
        val transliterationPaint = TextPaint().apply {
            color = Color.parseColor("#344E41")
            textSize = 34f
            typeface = banglaFont
            isAntiAlias = true
        }

        val transliterationLayout = if (!data.transliterationText.isNullOrBlank()) {
            StaticLayout.Builder.obtain(data.transliterationText.trim(), 0, data.transliterationText.trim().length, transliterationPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.25f)
                .setIncludePad(true)
                .build()
        } else null

        // 3. Translation Layout
        val cleanTranslation = try {
            android.text.Html.fromHtml(data.translationText.trim(), android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Exception) {
            data.translationText.trim()
        }

        val translationPaint = TextPaint().apply {
            color = Color.parseColor("#263D35") // Deep refined forest green
            textSize = when {
                cleanTranslation.length > 300 -> 34f
                cleanTranslation.length > 150 -> 38f
                else -> 42f
            }
            typeface = banglaFont
            isAntiAlias = true
        }

        val translationLayout = StaticLayout.Builder.obtain(cleanTranslation, 0, cleanTranslation.length, translationPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.35f)
            .setIncludePad(true)
            .build()

        // Calculate card height required
        val topPaddingInCard = 145f // Space below Islamic top arch
        var cardInnerContentHeight = topPaddingInCard

        if (arabicLayout != null) {
            cardInnerContentHeight += arabicLayout.height + 40f // Arabic text
            cardInnerContentHeight += 38f // Divider gap
        }

        if (transliterationLayout != null) {
            cardInnerContentHeight += transliterationLayout.height + 30f
            cardInnerContentHeight += 30f // Divider gap
        }

        cardInnerContentHeight += translationLayout.height + 45f

        if (!data.referenceText.isNullOrBlank()) {
            cardInnerContentHeight += 80f // Reference pill
        }

        cardInnerContentHeight += 70f // Bottom inner padding

        val cardTop = 195f
        val cardBottom = cardTop + max(cardInnerContentHeight, 580f)
        val footerSpace = 290f
        val totalCanvasHeight = (cardBottom + footerSpace).toInt().coerceAtLeast(1250)

        // Create bitmap and canvas
        val bitmap = Bitmap.createBitmap(width, totalCanvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // ==========================================
        // 1. OUTER BACKGROUND (Deep Emerald Gradient)
        // ==========================================
        val bgGradient = LinearGradient(
            0f, 0f, 0f, totalCanvasHeight.toFloat(),
            intArrayOf(
                Color.parseColor("#063D2D"), // Top Primary Emerald
                Color.parseColor("#07523D"), // Middle Rich Emerald
                Color.parseColor("#02291F")  // Bottom Deep Forest
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply { shader = bgGradient }
        canvas.drawRect(0f, 0f, width.toFloat(), totalCanvasHeight.toFloat(), bgPaint)

        // Subtle geometric star pattern
        drawIslamicPatternOverlay(canvas, width, totalCanvasHeight)

        // Draw Mosque Skyline Silhouette at bottom
        val mosquePaint = Paint().apply {
            color = Color.parseColor("#011710")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val mosquePath = Path().apply {
            val base = totalCanvasHeight.toFloat()
            moveTo(0f, base)
            lineTo(0f, base - 140f)
            // Left Minaret
            lineTo(45f, base - 140f)
            lineTo(55f, base - 250f)
            lineTo(65f, base - 140f)
            lineTo(140f, base - 140f)
            // Left Main Dome
            quadTo(240f, base - 280f, 340f, base - 140f)
            lineTo(450f, base - 140f)
            // Center Grand Dome
            quadTo(540f, base - 290f, 630f, base - 140f)
            lineTo(740f, base - 140f)
            // Right Main Dome
            quadTo(840f, base - 280f, 940f, base - 140f)
            lineTo(1015f, base - 140f)
            // Right Minaret
            lineTo(1025f, base - 250f)
            lineTo(1035f, base - 140f)
            lineTo(width.toFloat(), base - 140f)
            lineTo(width.toFloat(), base)
            close()
        }
        canvas.drawPath(mosquePath, mosquePaint)

        // ==========================================
        // TOP RIGHT HANGING ISLAMIC LANTERN
        // ==========================================
        val lanternX = 940f
        val chainPaint = Paint().apply {
            color = Color.parseColor("#E8D58A")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(lanternX, 0f, lanternX, 130f, chainPaint)

        // Lantern Warm Radial Glow
        val lanternGlow = RadialGradient(
            lanternX, 205f, 130f,
            intArrayOf(
                Color.parseColor("#FFFFE0"),
                Color.parseColor("#F5D77F"),
                Color.parseColor("#60F5D77F"),
                Color.parseColor("#00000000")
            ),
            floatArrayOf(0f, 0.25f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        val glowPaint = Paint().apply {
            shader = lanternGlow
            isAntiAlias = true
        }
        canvas.drawCircle(lanternX, 205f, 130f, glowPaint)

        // Lantern Gold Body
        val lanternGoldPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val capPath = Path().apply {
            moveTo(lanternX - 28f, 130f)
            lineTo(lanternX + 28f, 130f)
            lineTo(lanternX + 38f, 155f)
            lineTo(lanternX - 38f, 155f)
            close()
        }
        canvas.drawPath(capPath, lanternGoldPaint)

        // Glass Body
        val lanternGlassPath = Path().apply {
            moveTo(lanternX - 38f, 155f)
            lineTo(lanternX + 38f, 155f)
            lineTo(lanternX + 26f, 235f)
            lineTo(lanternX - 26f, 235f)
            close()
        }
        val glassPaint = Paint().apply {
            color = Color.parseColor("#80FFF6C2")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(lanternGlassPath, glassPaint)
        
        // Lantern Ribs
        canvas.drawLine(lanternX - 38f, 155f, lanternX - 26f, 235f, chainPaint)
        canvas.drawLine(lanternX, 155f, lanternX, 235f, chainPaint)
        canvas.drawLine(lanternX + 38f, 155f, lanternX + 26f, 235f, chainPaint)

        // Lantern Base & Tassel
        val baseRect = RectF(lanternX - 32f, 235f, lanternX + 32f, 248f)
        canvas.drawRoundRect(baseRect, 5f, 5f, lanternGoldPaint)
        canvas.drawLine(lanternX, 248f, lanternX, 275f, chainPaint)

        // ==========================================
        // TOP LEFT LUSH LEAVES VINE
        // ==========================================
        val stemPaint = Paint().apply {
            color = Color.parseColor("#123D24")
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        val stemPath = Path().apply {
            moveTo(-20f, -20f)
            cubicTo(30f, 25f, 80f, 50f, 165f, 95f)
        }
        canvas.drawPath(stemPath, stemPaint)

        val subStemPath = Path().apply {
            moveTo(55f, 38f)
            cubicTo(85f, 15f, 125f, 10f, 160f, 15f)
        }
        canvas.drawPath(subStemPath, stemPaint)

        val leavesData = listOf(
            listOf(10f, 8f, 30f, 75f, 28f),
            listOf(35f, 25f, 75f, 60f, 24f),
            listOf(55f, 38f, -25f, 80f, 30f),
            listOf(80f, 22f, 15f, 65f, 25f),
            listOf(115f, 14f, -15f, 70f, 26f),
            listOf(155f, 15f, -35f, 55f, 22f),
            listOf(85f, 55f, 80f, 65f, 25f),
            listOf(125f, 75f, 35f, 70f, 27f),
            listOf(165f, 95f, 50f, 60f, 23f)
        )

        for (leaf in leavesData) {
            drawRealisticLeaf(
                canvas,
                leaf[0].toFloat(),
                leaf[1].toFloat(),
                leaf[2].toFloat(),
                leaf[3].toFloat(),
                leaf[4].toFloat()
            )
        }

        // ==========================================
        // 2. INNER IVORY SCALLOPED ARCHED CARD
        // ==========================================
        val cardLeft = marginX
        val cardRight = width - marginX

        // Multi-lobed Islamic Arch Path
        val cardPath = Path().apply {
            val shoulderY = cardTop + 140f
            moveTo(cardLeft + 36f, cardBottom)
            quadTo(cardLeft, cardBottom, cardLeft, cardBottom - 36f)
            lineTo(cardLeft, shoulderY)
            
            // Scalloped multi-lobed arch
            cubicTo(
                cardLeft + 15f, cardTop + 60f,
                cardLeft + 80f, cardTop + 45f,
                cardLeft + 140f, cardTop + 25f
            )
            cubicTo(
                cardLeft + 200f, cardTop + 5f,
                540f - 120f, cardTop - 45f,
                540f, cardTop - 55f
            )
            cubicTo(
                540f + 120f, cardTop - 45f,
                cardRight - 200f, cardTop + 5f,
                cardRight - 140f, cardTop + 25f
            )
            cubicTo(
                cardRight - 80f, cardTop + 45f,
                cardRight - 15f, cardTop + 60f,
                cardRight, shoulderY
            )
            
            lineTo(cardRight, cardBottom - 36f)
            quadTo(cardRight, cardBottom, cardRight - 36f, cardBottom)
            close()
        }

        // Fill Inner Card with Warm Cream/Ivory Gradient
        val cardBgGradient = LinearGradient(
            0f, cardTop, 0f, cardBottom,
            intArrayOf(
                Color.parseColor("#FFFDF5"), // Cream Light Top
                Color.parseColor("#FAF4E8"), // Warm Cream Middle
                Color.parseColor("#F3ECDC")  // Soft Ivory Bottom
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        val cardBgPaint = Paint().apply {
            shader = cardBgGradient
            isAntiAlias = true
        }
        canvas.drawPath(cardPath, cardBgPaint)

        // Outer Metallic Gold Border
        val goldShader = LinearGradient(
            cardLeft, cardTop, cardRight, cardBottom,
            intArrayOf(
                Color.parseColor("#E8D58A"),
                Color.parseColor("#D4AF37"),
                Color.parseColor("#FCF6BA"),
                Color.parseColor("#C5A059"),
                Color.parseColor("#FBF5B7")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )

        val goldBorderPaint = Paint().apply {
            shader = goldShader
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawPath(cardPath, goldBorderPaint)

        // Inner Fine Gold Outline Frame
        val innerCardPath = Path().apply {
            val offset = 14f
            val iLeft = cardLeft + offset
            val iRight = cardRight - offset
            val iTop = cardTop + offset
            val iBottom = cardBottom - offset
            val shoulderY = iTop + 130f

            moveTo(iLeft + 28f, iBottom)
            quadTo(iLeft, iBottom, iLeft, iBottom - 28f)
            lineTo(iLeft, shoulderY)
            cubicTo(
                iLeft + 15f, iTop + 55f,
                iLeft + 75f, iTop + 40f,
                iLeft + 130f, iTop + 22f
            )
            cubicTo(
                iLeft + 190f, iTop + 4f,
                540f - 110f, iTop - 42f,
                540f, iTop - 50f
            )
            cubicTo(
                540f + 110f, iTop - 42f,
                iRight - 190f, iTop + 4f,
                iRight - 130f, iTop + 22f
            )
            cubicTo(
                iRight - 75f, iTop + 40f,
                iRight - 15f, iTop + 55f,
                iRight, shoulderY
            )
            lineTo(iRight, iBottom - 28f)
            quadTo(iRight, iBottom, iRight - 28f, iBottom)
            close()
        }

        val innerGoldPaint = Paint().apply {
            shader = goldShader
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawPath(innerCardPath, innerGoldPaint)

        // Corner Gold Floral Flourishes
        val goldFlourishPaint = Paint().apply {
            shader = goldShader
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        drawGoldFloralFlourish(canvas, cardLeft + 52f, cardTop + 160f, 22f, goldFlourishPaint)
        drawGoldFloralFlourish(canvas, cardRight - 52f, cardTop + 160f, 22f, goldFlourishPaint)
        drawGoldFloralFlourish(canvas, cardLeft + 52f, cardBottom - 48f, 22f, goldFlourishPaint)
        drawGoldFloralFlourish(canvas, cardRight - 52f, cardBottom - 48f, 22f, goldFlourishPaint)

        // ==========================================
        // 3. TOP SCALLOPED ISLAMIC TITLE BADGE
        // ==========================================
        // Parse Title and optional Subtitle e.g. "আয়াতে শিফা (রোগমুক্তির আয়াত)"
        val rawBadge = data.badgeTitle.trim()
        val parsedTitle: String
        val parsedSubtitle: String?

        if (data.subtitle != null && data.subtitle.isNotBlank()) {
            parsedTitle = rawBadge
            parsedSubtitle = data.subtitle.trim()
        } else if (rawBadge.contains("(") && rawBadge.contains(")")) {
            val startIdx = rawBadge.indexOf("(")
            parsedTitle = rawBadge.substring(0, startIdx).trim()
            parsedSubtitle = rawBadge.substring(startIdx).trim()
        } else if (rawBadge.contains("•")) {
            val parts = rawBadge.split("•", limit = 2)
            parsedTitle = parts[0].trim()
            parsedSubtitle = parts[1].trim()
        } else {
            parsedTitle = rawBadge
            parsedSubtitle = null
        }

        val badgeMainPaint = TextPaint().apply {
            color = Color.parseColor("#E8D58A") // Soft Metallic Gold
            textSize = 36f
            typeface = banglaBoldFont
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val badgeSubPaint = TextPaint().apply {
            color = Color.parseColor("#FFFDF5") // Warm Cream
            textSize = 26f
            typeface = banglaFont
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val titleWidth = badgeMainPaint.measureText(parsedTitle)
        val subWidth = parsedSubtitle?.let { badgeSubPaint.measureText(it) } ?: 0f
        val maxBadgeTextWidth = max(titleWidth, subWidth)

        val badgeWidth = (maxBadgeTextWidth + 140f).coerceAtLeast(360f)
        val badgeHeight = if (parsedSubtitle != null) 92f else 66f
        val badgeCenterY = cardTop - 45f

        val badgeRect = RectF(
            540f - badgeWidth / 2f,
            badgeCenterY - badgeHeight / 2f,
            540f + badgeWidth / 2f,
            badgeCenterY + badgeHeight / 2f
        )

        val badgeBgPaint = Paint().apply {
            color = Color.parseColor("#063D2D")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeBorderPaint = Paint().apply {
            shader = goldShader
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            isAntiAlias = true
        }

        // Draw Scalloped Arched Badge
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, badgeBorderPaint)

        // Side Gold Star Ornaments on Badge
        val starPaint = TextPaint().apply {
            color = Color.parseColor("#E8D58A")
            textSize = 28f
            typeface = banglaBoldFont
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦", badgeRect.left + 28f, badgeCenterY + 10f, starPaint)
        canvas.drawText("✦", badgeRect.right - 28f, badgeCenterY + 10f, starPaint)

        if (parsedSubtitle != null) {
            canvas.drawText(parsedTitle, 540f, badgeCenterY - 6f, badgeMainPaint)
            canvas.drawText(parsedSubtitle, 540f, badgeCenterY + 28f, badgeSubPaint)
        } else {
            canvas.drawText(parsedTitle, 540f, badgeCenterY + 12f, badgeMainPaint)
        }

        // ==========================================
        // 4. DRAW CONTENT INSIDE CARD
        // ==========================================
        var currentY = cardTop + 100f

        // 4a. Arabic Text & Side Mandala Flourishes
        if (arabicLayout != null) {
            val arabicCenterY = currentY + arabicLayout.height / 2f

            // Side delicate gold mandala blossoms
            drawGoldFloralFlourish(canvas, marginX + 38f, arabicCenterY, 18f, goldFlourishPaint)
            drawGoldFloralFlourish(canvas, width - marginX - 38f, arabicCenterY, 18f, goldFlourishPaint)

            canvas.save()
            canvas.translate(marginX + contentPaddingX, currentY)
            arabicLayout.draw(canvas)
            canvas.restore()

            currentY += arabicLayout.height + 30f

            // Elegant Gold Ornamental Divider
            val dividerPaint = Paint().apply {
                shader = goldShader
                strokeWidth = 2.2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(540f - 140f, currentY, 540f - 24f, currentY, dividerPaint)
            canvas.drawLine(540f + 24f, currentY, 540f + 140f, currentY, dividerPaint)

            val divSymbolPaint = TextPaint().apply {
                color = Color.parseColor("#D4AF37")
                textSize = 26f
                typeface = banglaBoldFont
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("✦", 540f, currentY + 8f, divSymbolPaint)
            currentY += 45f
        }

        // 4b. Transliteration / Pronunciation
        if (transliterationLayout != null) {
            canvas.save()
            canvas.translate(marginX + contentPaddingX, currentY)
            transliterationLayout.draw(canvas)
            canvas.restore()

            currentY += transliterationLayout.height + 25f

            val smallDotPaint = TextPaint().apply {
                color = Color.parseColor("#C5A059")
                textSize = 22f
                typeface = banglaFont
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("•• ❖ ••", 540f, currentY + 8f, smallDotPaint)
            currentY += 35f
        }

        // 4c. Bengali Translation
        canvas.save()
        canvas.translate(marginX + contentPaddingX, currentY)
        translationLayout.draw(canvas)
        canvas.restore()

        currentY += translationLayout.height + 40f

        // 4d. Reference Badge with Flourish Wings
        if (!data.referenceText.isNullOrBlank()) {
            val refText = data.referenceText.trim()
            val refPaint = TextPaint().apply {
                color = Color.parseColor("#FFFDF5") // Clean Cream
                textSize = 32f
                typeface = banglaBoldFont
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val iconPrefix = "📖  "
            val fullRefString = "$iconPrefix$refText"
            val refTextWidth = refPaint.measureText(fullRefString)
            val refPillWidth = refTextWidth + 64f
            val refPillHeight = 60f
            val refPillRect = RectF(
                540f - refPillWidth / 2f,
                currentY,
                540f + refPillWidth / 2f,
                currentY + refPillHeight
            )

            val refPillBg = Paint().apply {
                color = Color.parseColor("#063D2D") // Dark Emerald Pill
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val refPillBorder = Paint().apply {
                shader = goldShader
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }

            // Flourish horizontal wings outside the pill
            val wingPaint = Paint().apply {
                shader = goldShader
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            val pillCenterY = currentY + refPillHeight / 2f
            canvas.drawLine(refPillRect.left - 50f, pillCenterY, refPillRect.left - 8f, pillCenterY, wingPaint)
            canvas.drawLine(refPillRect.right + 8f, pillCenterY, refPillRect.right + 50f, pillCenterY, wingPaint)

            canvas.drawRoundRect(refPillRect, 30f, 30f, refPillBg)
            canvas.drawRoundRect(refPillRect, 30f, 30f, refPillBorder)
            canvas.drawText(fullRefString, 540f, currentY + 41f, refPaint)
        }

        // ==========================================
        // 5. BOTTOM BRANDING & SOCIAL LINKS SECTION
        // ==========================================
        val footerY = cardBottom + 35f

        // Draw Quran Reader Logo & Emblem
        val logoBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.credit)
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher)
        } catch (e: Exception) {
            null
        }

        val logoSize = 64f
        val logoCenterX = 540f
        val logoCenterY = footerY + logoSize / 2f

        if (logoBitmap != null) {
            val logoRect = RectF(logoCenterX - logoSize / 2f, footerY, logoCenterX + logoSize / 2f, footerY + logoSize)
            val logoBorderRect = RectF(logoCenterX - logoSize / 2f - 4f, footerY - 4f, logoCenterX + logoSize / 2f + 4f, footerY + logoSize + 4f)

            val logoBgPaint = Paint().apply {
                color = Color.parseColor("#063D2D")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val logoBorderPaint = Paint().apply {
                shader = goldShader
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                isAntiAlias = true
            }

            canvas.drawRoundRect(logoBorderRect, 22f, 22f, logoBgPaint)
            canvas.drawRoundRect(logoBorderRect, 22f, 22f, logoBorderPaint)
            canvas.drawBitmap(logoBitmap, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }

        // "Quran READER" typography in serif display
        val appNamePaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.footerAppName, 540f, footerY + logoSize + 42f, appNamePaint)

        // ==========================================
        // SOCIAL CREDITS PILL (FB & Telegram Links)
        // ==========================================
        val socialBarY = footerY + logoSize + 70f
        val socialTextPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = banglaBoldFont
            isAntiAlias = true
        }

        val fbText = "/MuslimsLibrary"
        val tgText = "/MuslimsLibraryApp"

        val fbTextWidth = socialTextPaint.measureText(fbText)
        val tgTextWidth = socialTextPaint.measureText(tgText)

        val iconSize = 36f
        val iconGap = 12f
        val itemGap = 32f
        val sectionDividerWidth = 28f

        val totalPillWidth = (iconSize + iconGap + fbTextWidth) + itemGap + sectionDividerWidth + itemGap + (iconSize + iconGap + tgTextWidth) + 90f
        val pillHeight = 56f

        val pillStartX = (width - totalPillWidth) / 2f
        val pillRect = RectF(pillStartX, socialBarY, pillStartX + totalPillWidth, socialBarY + pillHeight)

        val pillBgPaint = Paint().apply {
            color = Color.parseColor("#063D2D")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val pillBorderPaint = Paint().apply {
            shader = goldShader
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        canvas.drawRoundRect(pillRect, 28f, 28f, pillBgPaint)
        canvas.drawRoundRect(pillRect, 28f, 28f, pillBorderPaint)

        // Side Star Accents on Social Pill
        val pillStarPaint = TextPaint().apply {
            color = Color.parseColor("#E8D58A")
            textSize = 24f
            typeface = banglaBoldFont
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦", pillStartX + 20f, socialBarY + pillHeight / 2f + 8f, pillStarPaint)
        canvas.drawText("✦", pillStartX + totalPillWidth - 20f, socialBarY + pillHeight / 2f + 8f, pillStarPaint)

        var currentX = pillStartX + 42f

        // Facebook Icon (Blue Circle with white 'f')
        val fbCirclePaint = Paint().apply {
            color = Color.parseColor("#1877F2")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(currentX + iconSize / 2f, socialBarY + pillHeight / 2f, iconSize / 2f, fbCirclePaint)

        val fbFPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("f", currentX + iconSize / 2f, socialBarY + pillHeight / 2f + 9f, fbFPaint)

        currentX += iconSize + iconGap
        canvas.drawText(fbText, currentX, socialBarY + pillHeight / 2f + 10f, socialTextPaint)

        currentX += fbTextWidth + itemGap

        // Vertical Divider |
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#60D4AF37")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(currentX + sectionDividerWidth / 2f, socialBarY + 12f, currentX + sectionDividerWidth / 2f, socialBarY + pillHeight - 12f, dividerPaint)

        currentX += sectionDividerWidth + itemGap

        // Telegram Icon (Cyan Circle with white plane)
        val tgCirclePaint = Paint().apply {
            color = Color.parseColor("#229ED9")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(currentX + iconSize / 2f, socialBarY + pillHeight / 2f, iconSize / 2f, tgCirclePaint)

        // Draw Telegram paper plane vector icon
        val tgPlanePath = Path().apply {
            val cx = currentX + iconSize / 2f
            val cy = socialBarY + pillHeight / 2f
            moveTo(cx - 8f, cy + 1f)
            lineTo(cx + 9f, cy - 7f)
            lineTo(cx + 3f, cy + 8f)
            lineTo(cx + 1f, cy + 3f)
            close()
        }
        val tgPlanePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(tgPlanePath, tgPlanePaint)

        currentX += iconSize + iconGap
        canvas.drawText(tgText, currentX, socialBarY + pillHeight / 2f + 10f, socialTextPaint)

        return bitmap
    }
}

