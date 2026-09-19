package com.example.utils

import android.provider.MediaStore
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.ShortPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object PostShareUtil {

    private val bgImageCache = object : android.util.LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private val fontCache = java.util.concurrent.ConcurrentHashMap<Int, Typeface>()
    private var cachedLogoBitmap: Bitmap? = null

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    fun loadSampledBackgroundBitmap(context: Context, bgImageUrl: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val cacheKey = "${bgImageUrl}_${targetWidth}x${targetHeight}"
        val cached = bgImageCache.get(cacheKey) ?: bgImageCache.get(bgImageUrl)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return try {
            if (bgImageUrl.startsWith("content://") || bgImageUrl.startsWith("file://")) {
                val uri = android.net.Uri.parse(bgImageUrl)
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOptions)
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val decoded = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                }
                if (decoded != null) {
                    bgImageCache.put(cacheKey, decoded)
                }
                decoded
            } else {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val url = java.net.URL(bgImageUrl)
                val connection = url.openConnection().apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val bytes = connection.getInputStream().use { it.readBytes() }

                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                if (decoded != null) {
                    bgImageCache.put(cacheKey, decoded)
                }
                decoded
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun getCachedLogoBitmap(context: Context): Bitmap? {
        if (cachedLogoBitmap != null && !cachedLogoBitmap!!.isRecycled) {
            return cachedLogoBitmap
        }
        return try {
            val decoded = BitmapFactory.decodeResource(context.resources, R.drawable.credit)
            cachedLogoBitmap = decoded
            decoded
        } catch (e: Exception) {
            null
        }
    }

    private fun getCachedFont(context: Context, resId: Int): Typeface? {
        return fontCache.getOrPut(resId) {
            try {
                ResourcesCompat.getFont(context, resId) ?: Typeface.DEFAULT
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
    }

    private fun getCustomFontFromUri(context: Context, uriString: String): Typeface? {
        val key = uriString.hashCode()
        val cached = fontCache[key]
        if (cached != null) return cached

        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "temp_custom_font_${uriString.hashCode()}.ttf")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            val typeface = Typeface.createFromFile(tempFile)
            if (typeface != null) {
                fontCache[key] = typeface
            }
            typeface
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    data class ColorPreset(val name: String, val hex: String)
    val TextColorPresets = listOf(
        ColorPreset("সাদা", "#FFFFFF"),
        ColorPreset("ক্রিম", "#FEF3C7"),
        ColorPreset("স্বর্ণালী", "#FBBF24"),
        ColorPreset("অ্যাম্বার", "#D97706"),
        ColorPreset("এমারেল্ড", "#10B981"),
        ColorPreset("গাঢ় সবুজ", "#065F46"),
        ColorPreset("মিন্ট", "#6EE7B7"),
        ColorPreset("আকাশি", "#38BDF8"),
        ColorPreset("নীল", "#3B82F6"),
        ColorPreset("রোজ পিঙ্ক", "#FDA4AF"),
        ColorPreset("রক্তিম", "#E11D48"),
        ColorPreset("বালুকা", "#FCD34D"),
        ColorPreset("হালকা ছাই", "#E2E8F0"),
        ColorPreset("ধূসর", "#94A3B8"),
        ColorPreset("ডার্ক স্লেট", "#1E293B"),
        ColorPreset("কালো", "#000000")
    )

    enum class TemplateCategory(val title: String) {
        ALL("সব"),
        QURAN("কুরআনের আয়াত"),
        HADITH("হাদিস"),
        DUA("দোয়া"),
        NASIHAT("দৈনিক নসীহত"),
        JUMUAH("জুমা"),
        RAMADAN("রমজান"),
        QUOTE("ইসলামিক উক্তি")
    }

    data class CardTemplate(
        val id: String,
        val title: String,
        val categories: List<TemplateCategory>,
        val bgColors: Pair<String, String>,
        val textColor: String,
        val titleColor: String,
        val referenceColor: String,
        val borderColor: String,
        val showBorder: Boolean = true,
        val defaultTextAlign: String = "CENTER",
        val defaultFontName: String = "SolaimanLipi",
        val defaultFontSize: Float = 44f,
        val defaultLineSpacing: Float = 1.15f,
        val defaultOverlayAlpha: Float = 0.70f,
        val showLogo: Boolean = true,
        val showWatermark: Boolean = true
    )
    val preDefinedTemplates = listOf(
        CardTemplate("emerald", "Emerald Islamic", listOf(TemplateCategory.ALL, TemplateCategory.QURAN, TemplateCategory.NASIHAT, TemplateCategory.RAMADAN), Pair("#063D2D", "#02291F"), "#263D35", "#E8D58A", "#FFFDF5", "#D4AF37", showBorder = true),
        CardTemplate("royal_night", "Royal Night", listOf(TemplateCategory.ALL, TemplateCategory.QURAN, TemplateCategory.RAMADAN), Pair("#172554", "#0F172A"), "#F8FAFC", "#FBBF24", "#FEF3C7", "#30FBBF24", showBorder = true),
        CardTemplate("classic_cream", "Classic Cream", listOf(TemplateCategory.ALL, TemplateCategory.HADITH, TemplateCategory.QUOTE), Pair("#FFFBEB", "#FEF3C7"), "#064E3B", "#D97706", "#D97706", "#30D97706", showBorder = true, defaultTextAlign = "LEFT", defaultFontName = "Hind Siliguri", defaultFontSize = 40f, defaultOverlayAlpha = 0.85f),
        CardTemplate("minimal", "Minimal", listOf(TemplateCategory.ALL, TemplateCategory.QUOTE), Pair("#F8FAFC", "#F1F5F9"), "#0F172A", "#475569", "#475569", "#00000000", showBorder = false, defaultTextAlign = "LEFT", defaultFontName = "Hind Siliguri", defaultFontSize = 46f, defaultOverlayAlpha = 0.90f),
        CardTemplate("golden_hadith", "Golden Hadith", listOf(TemplateCategory.ALL, TemplateCategory.HADITH), Pair("#3B1700", "#78350F"), "#FEF3C7", "#FBBF24", "#FBBF24", "#30FBBF24", showBorder = true, defaultFontSize = 42f),
        CardTemplate("jumuah", "Jumu'ah", listOf(TemplateCategory.ALL, TemplateCategory.JUMUAH), Pair("#042F2E", "#115E59"), "#CCFBF1", "#2DD4BF", "#2DD4BF", "#302DD4BF", showBorder = true, defaultFontSize = 48f),
        CardTemplate("quran_classic", "Quran Classic", listOf(TemplateCategory.ALL, TemplateCategory.QURAN, TemplateCategory.DUA), Pair("#2E1065", "#4C1D95"), "#F3E8FF", "#C084FC", "#C084FC", "#30C084FC", showBorder = true)
    )

    /**
     * Intelligently selects the most relevant template and category for a given post or draft content.
     */
    fun findBestTemplateForPost(post: ShortPost): Pair<TemplateCategory, CardTemplate> {
        val catLower = post.category.lowercase()
        val textLower = post.text.lowercase()
        val refLower = post.reference.lowercase()

        val category = when {
            catLower.contains("কুরআন") || catLower.contains("আয়াত") || catLower.contains("quran") || catLower.contains("ayah") || refLower.contains("সূরা") -> TemplateCategory.QURAN
            catLower.contains("হাদিস") || catLower.contains("হাদীস") || catLower.contains("hadith") || refLower.contains("বুখারী") || refLower.contains("মুসলিম") || refLower.contains("তিরমিজী") -> TemplateCategory.HADITH
            catLower.contains("দোয়া") || catLower.contains("দোয়া") || catLower.contains("মোনাজাত") || catLower.contains("dua") -> TemplateCategory.DUA
            catLower.contains("জুমা") || catLower.contains("jumu") -> TemplateCategory.JUMUAH
            catLower.contains("রমজান") || catLower.contains("রোজা") || catLower.contains("ramadan") -> TemplateCategory.RAMADAN
            catLower.contains("উক্তি") || catLower.contains("বাণী") || catLower.contains("quote") -> TemplateCategory.QUOTE
            catLower.contains("নসীহত") || catLower.contains("nasihat") -> TemplateCategory.NASIHAT
            else -> TemplateCategory.ALL
        }

        val template = when (category) {
            TemplateCategory.QURAN -> preDefinedTemplates.find { it.id == "royal_night" } ?: preDefinedTemplates.find { it.id == "quran_classic" } ?: preDefinedTemplates.first()
            TemplateCategory.HADITH -> preDefinedTemplates.find { it.id == "golden_hadith" } ?: preDefinedTemplates.find { it.id == "classic_cream" } ?: preDefinedTemplates.first()
            TemplateCategory.DUA -> preDefinedTemplates.find { it.id == "emerald" } ?: preDefinedTemplates.first()
            TemplateCategory.JUMUAH -> preDefinedTemplates.find { it.id == "jumuah" } ?: preDefinedTemplates.first()
            TemplateCategory.RAMADAN -> preDefinedTemplates.find { it.id == "emerald" } ?: preDefinedTemplates.first()
            TemplateCategory.QUOTE -> preDefinedTemplates.find { it.id == "classic_cream" } ?: preDefinedTemplates.find { it.id == "minimal" } ?: preDefinedTemplates.first()
            TemplateCategory.NASIHAT -> preDefinedTemplates.find { it.id == "emerald" } ?: preDefinedTemplates.first()
            TemplateCategory.ALL -> preDefinedTemplates.first()
        }

        return Pair(category, template)
    }

    fun buildShareText(post: ShortPost): String {
        return buildString {
            append("✨ ").append(post.category).append(" ✨\n\n")
            append(post.text).append("\n\n")
            if (post.reference.isNotEmpty()) {
                append("সূত্র: ").append(post.reference).append("\n")
            }
            append("\n---\n")
            append("📱 ❝কুরআন রিডার❞ অ্যাপ থেকে শেয়ারকৃত।")
        }
    }

    fun copyToClipboard(context: Context, post: ShortPost) {
        val shareText = buildShareText(post)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Short Post Text", shareText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "লেখাটি ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
    }

    fun shareAsText(context: Context, post: ShortPost) {
        val shareText = buildShareText(post)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "পোস্ট শেয়ার করুন (টেক্সট)"))
    }
    suspend fun generateCardBitmap(
        context: Context,
        post: ShortPost,
        template: CardTemplate = preDefinedTemplates.first(),
        bgImageUrl: String? = null,
        overlayAlpha: Float = 0.70f,
        aspectRatio: String = "1:1",
        customOverlayColor: String? = null,
        textAlignName: String = "CENTER",
        fontName: String = "SolaimanLipi",
        fontSizeSp: Float = 44f,
        lineSpacingMult: Float = 1.15f,
        customCategory: String? = null,
        customText: String? = null,
        customRef: String? = null,
        customTitleColor: String? = null,
        customTextColor: String? = null,
        customRefColor: String? = null,
        showLogo: Boolean = true,
        autoFitText: Boolean = true,
        showWatermark: Boolean = true,
        textWidthPercent: Float = 1f,
        textLetterSpacing: Float = 0f,
        isTextBold: Boolean = false,
        isForPreview: Boolean = false
    ): Bitmap = withContext(Dispatchers.IO) {
        val baseWidth = if (isForPreview) 540 else 1080
        val scale = baseWidth / 1080f

        val width = baseWidth
        val margin = (80 * scale).toInt()
        val baseContentWidth = width - 2 * margin
        val contentWidth = (baseContentWidth * textWidthPercent).toInt()
        val textLayoutMarginX = margin + (baseContentWidth - contentWidth) / 2f

        val displayCategory: String = customCategory?.takeIf { it.isNotBlank() } ?: post.category
        val displayText: String = customText?.takeIf { it.isNotBlank() } ?: post.text
        val displayRef: String = customRef?.takeIf { it.isNotBlank() } ?: post.reference

        if (bgImageUrl.isNullOrBlank() && template.id == "emerald") {
            var extractedArabic: String? = null
            var extractedTranslation = displayText

            val paragraphs = displayText.split("\n\n")
            val arabicParagraphs = paragraphs.filter { p -> p.any { isArabicChar(it) } }
            val nonArabicParagraphs = paragraphs.filter { p -> !p.any { isArabicChar(it) } }

            if (arabicParagraphs.isNotEmpty() && nonArabicParagraphs.isNotEmpty()) {
                extractedArabic = arabicParagraphs.joinToString("\n\n").trim()
                extractedTranslation = nonArabicParagraphs.joinToString("\n\n").trim()
            } else if (arabicParagraphs.isNotEmpty() && nonArabicParagraphs.isEmpty()) {
                extractedArabic = displayText.trim()
                extractedTranslation = ""
            }

            val shareData = IslamicCardTemplate.ShareData(
                badgeTitle = displayCategory.ifBlank { "আজকের পোস্ট" },
                arabicText = extractedArabic,
                transliterationText = null,
                translationText = extractedTranslation,
                referenceText = displayRef.ifBlank { null }
            )
            val fullBitmap = IslamicCardTemplate.generateCardBitmap(context, shareData)
            return@withContext if (isForPreview) {
                try {
                    Bitmap.createScaledBitmap(fullBitmap, fullBitmap.width / 2, fullBitmap.height / 2, true)
                } catch (e: Throwable) {
                    fullBitmap
                }
            } else {
                fullBitmap
            }
        }

        // Load Shahrazad font for Arabic script
        val shahrazadFont = getCachedFont(context, R.font.scheherazade_new) ?: Typeface.DEFAULT

        // Load custom Bangla font based on selection
        val chosenFont = try {
            if (fontName.startsWith("content://") || fontName.startsWith("file://")) {
                getCustomFontFromUri(context, fontName) ?: shahrazadFont
            } else {
                when (fontName) {
                    "Scheherazade New", "Scheherazade", "Shahrazad", "শাহরাজাদ" -> shahrazadFont
                    "Amiri" -> getCachedFont(context, R.font.amiri_regular) ?: shahrazadFont
                    "Hind Siliguri" -> getCachedFont(context, R.font.hind_siliguri)
                    "Shorif Shishir Unicode", "Shorif Shishir" -> getCachedFont(context, R.font.shorif_shishir)
                    "SolaimanLipi" -> getCachedFont(context, R.font.solaimanlipi)
                    "Default" -> Typeface.DEFAULT
                    else -> getCachedFont(context, R.font.solaimanlipi)
                } ?: Typeface.DEFAULT
            }
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val chosenBoldFont = try {
            when (fontName) {
                "SolaimanLipi" -> getCachedFont(context, R.font.solaimanlipi_bold) ?: chosenFont
                else -> chosenFont
            }
        } catch (e: Exception) {
            chosenFont
        }

        // Alignments
        val paintAlign = when (textAlignName) {
            "LEFT" -> Paint.Align.LEFT
            "RIGHT" -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }

        val staticLayoutAlign = when (textAlignName) {
            "LEFT" -> Layout.Alignment.ALIGN_NORMAL
            "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }

        val hasArabicCategory = displayCategory.any { isArabicChar(it) }
        val categoryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(customTitleColor ?: template.titleColor)
            textSize = 34f * scale
            typeface = if (hasArabicCategory) shahrazadFont else chosenBoldFont
            textAlign = paintAlign
            if (isTextBold) isFakeBoldText = true
            letterSpacing = textLetterSpacing
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(customTextColor ?: template.textColor)
            textSize = fontSizeSp * scale
            typeface = if (isTextBold) chosenBoldFont else chosenFont
            if (isTextBold) isFakeBoldText = true
            textAlign = Paint.Align.LEFT // CRITICAL: StaticLayout requires Align.LEFT; alignment is handled by StaticLayout.Alignment
            letterSpacing = textLetterSpacing
        }

        val hasArabicRef = displayRef.any { isArabicChar(it) }
        val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(customRefColor ?: template.referenceColor)
            textSize = 32f * scale
            typeface = if (hasArabicRef) shahrazadFont else chosenFont
            textAlign = paintAlign
            if (isTextBold) isFakeBoldText = true
            letterSpacing = textLetterSpacing
        }

        val creditPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(customTextColor ?: template.textColor).run {
                Color.argb(170, Color.red(this), Color.green(this), Color.blue(this))
            }
            textSize = 28f * scale
            typeface = chosenBoldFont
            textAlign = Paint.Align.CENTER
        }

        // Apply Shahrazad font automatically to Arabic characters/text
        val formattedDisplayText = formatTextWithArabicFont(displayText, shahrazadFont)

        var currentFontSize = fontSizeSp * scale
        val minFontSize = 20f * scale
        var textLayout: StaticLayout
        var textHeight: Float
        val categoryText = if (displayCategory.isNotBlank()) "— $displayCategory —" else ""
        val refText = if (displayRef.isNotEmpty()) "— $displayRef —" else ""
        val creditText = "📱 ❝কুরআন রিডার❞ অ্যাপ থেকে সংগৃহীত"
        val categoryHeight = if (categoryText.isNotEmpty()) 50f * scale else 0f
        val gap1 = if (categoryText.isNotEmpty()) 30f * scale else 0f
        val gap2 = if (refText.isNotEmpty()) 30f * scale else 0f
        val refHeight = if (refText.isNotEmpty()) 45f * scale else 0f
        val topHeaderSpace = 130f * scale  // Reserved at top for border and top-left credit logo
        val bottomFooterSpace = 130f * scale // Reserved at bottom for divider line and credit watermark
        val baseStandardHeight = when(aspectRatio) {
            "4:5" -> 1350
            "9:16" -> 1920
            "16:9" -> 608
            else -> 1080
        }
        val fixedStandardHeight = (baseStandardHeight * scale).toInt()
        val maxMiddleHeight = fixedStandardHeight - topHeaderSpace - bottomFooterSpace

        while (true) {
            textPaint.textSize = currentFontSize
            textLayout = StaticLayout.Builder.obtain(
                formattedDisplayText, 0, formattedDisplayText.length, textPaint, contentWidth
            ).setAlignment(staticLayoutAlign).setLineSpacing(12f * scale, lineSpacingMult).build()
            
            textHeight = textLayout.height.toFloat()
            val currentMiddleHeight = categoryHeight + gap1 + textHeight + gap2 + refHeight
            
            if (autoFitText && currentMiddleHeight > maxMiddleHeight && currentFontSize > minFontSize) {
                currentFontSize -= (2f * scale)
            } else {
                break
            }
        }

        val middleContentHeight = categoryHeight + gap1 + textHeight + gap2 + refHeight
        val requiredHeight = (topHeaderSpace + middleContentHeight + bottomFooterSpace).toInt()
        val finalHeight = requiredHeight.coerceAtLeast(fixedStandardHeight)

        val bitmap = try {
            Bitmap.createBitmap(width, finalHeight, Bitmap.Config.ARGB_8888)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            // Fallback to smaller dimension on extreme memory pressure
            Bitmap.createBitmap(width / 2, finalHeight / 2, Bitmap.Config.RGB_565)
        }
        val canvas = Canvas(bitmap)

        // 1. Optional background image from URL or Gallery (Memory Efficient via loadSampledBackgroundBitmap)
        var bgBitmapDrawn = false
        if (!bgImageUrl.isNullOrBlank()) {
            try {
                val loadedBg = loadSampledBackgroundBitmap(context, bgImageUrl, width, finalHeight)
                if (loadedBg != null && !loadedBg.isRecycled) {
                    val imgRatio = loadedBg.width.toFloat() / loadedBg.height.toFloat()
                    val canvasRatio = width.toFloat() / finalHeight.toFloat()

                    var srcX = 0
                    var srcY = 0
                    var srcW = loadedBg.width
                    var srcH = loadedBg.height

                    if (imgRatio > canvasRatio) {
                        srcW = (loadedBg.height * canvasRatio).toInt()
                        srcX = (loadedBg.width - srcW) / 2
                    } else {
                        srcH = (loadedBg.width / canvasRatio).toInt()
                        srcY = (loadedBg.height - srcH) / 2
                    }

                    val srcRect = Rect(srcX, srcY, srcX + srcW, srcY + srcH)
                    val dstRect = Rect(0, 0, width, finalHeight)
                    canvas.drawBitmap(loadedBg, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                    bgBitmapDrawn = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Gradient / Solid Color Overlay
        val topOverlayColor = customOverlayColor?.let { Color.parseColor(it) } ?: Color.parseColor(template.bgColors.first)
        val bottomOverlayColor = customOverlayColor?.let { Color.parseColor(it) } ?: Color.parseColor(template.bgColors.second)

        val shader = LinearGradient(
            0f, 0f, 0f, finalHeight.toFloat(),
            topOverlayColor,
            bottomOverlayColor,
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply {
            this.shader = shader
            if (bgBitmapDrawn) {
                alpha = (overlayAlpha * 255).toInt().coerceIn(0, 255)
            }
        }
        canvas.drawRect(0f, 0f, width.toFloat(), finalHeight.toFloat(), bgPaint)

        // 3. Rounded Decorative Inner Border
        if (template.showBorder) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(template.borderColor)
                style = Paint.Style.STROKE
                strokeWidth = 4f * scale
            }
            val borderPadding = 32f * scale
            val borderCorner = 28f * scale
            canvas.drawRoundRect(RectF(borderPadding, borderPadding, (width - borderPadding).toFloat(), (finalHeight - borderPadding).toFloat()), borderCorner, borderCorner, borderPaint)
        }

        // 4. Top-Left Credit Logo (credit.png) - Conditional & Cached
        if (showLogo) {
            try {
                val logoBitmap = getCachedLogoBitmap(context)
                if (logoBitmap != null && !logoBitmap.isRecycled) {
                    val logoSize = 64f * scale
                    val logoLeft = 52f * scale
                    val logoTop = 52f * scale
                    val dstRect = RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
                    val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(logoBitmap, null, dstRect, logoPaint)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 5. Middle Content Vertical Centering
        val availableMiddleHeight = finalHeight.toFloat() - topHeaderSpace - bottomFooterSpace
        val middleStartY = topHeaderSpace + ((availableMiddleHeight - middleContentHeight) / 2f).coerceAtLeast(0f)
        var currentY = middleStartY + (if (categoryText.isNotEmpty()) 35f * scale else 0f)

        val alignX = when (textAlignName) {
            "LEFT" -> margin.toFloat()
            "RIGHT" -> (width - margin).toFloat()
            else -> (width / 2).toFloat()
        }

        // Category Tag
        if (categoryText.isNotEmpty()) {
            canvas.drawText(categoryText, alignX, currentY, categoryPaint)
            currentY += gap1 + (15f * scale)
        }

        // Main Text Layout
        canvas.save()
        canvas.translate(textLayoutMarginX, currentY)
        textLayout.draw(canvas)
        canvas.restore()
        currentY += textHeight + gap2

        // Reference
        if (refText.isNotEmpty()) {
            currentY += (15f * scale)
            canvas.drawText(refText, alignX, currentY, refPaint)
        }

        // 6. Bottom Credit Watermark & Divider Line - Conditional
        if (showWatermark) {
            val footerDividerY = finalHeight.toFloat() - (110f * scale)
            val footerCreditY = finalHeight.toFloat() - (60f * scale)
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(template.borderColor)
                strokeWidth = 2f * scale
            }
            val dividerInset = 150f * scale
            canvas.drawLine(dividerInset, footerDividerY, (width - dividerInset).toFloat(), footerDividerY, linePaint)

            // Watermark Credit
            canvas.drawText(creditText, (width / 2).toFloat(), footerCreditY, creditPaint)
        }

        bitmap
    }

    suspend fun shareBitmap(context: Context, bitmap: Bitmap) {
        try {
            val cacheDir = File(context.cacheDir, "shared_posts")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, "post_${System.currentTimeMillis()}.png")
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
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "ফটো কার্ড শেয়ার করুন"))
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ফটো কার্ড শেয়ার করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    suspend fun shareAsImage(
        context: Context,
        post: ShortPost,
        template: CardTemplate = preDefinedTemplates.first(),
        bgImageUrl: String? = null,
        aspectRatio: String = "1:1",
        customOverlayColor: String? = null,
        overlayAlpha: Float = 0.70f,
        textAlignName: String = "CENTER",
        fontName: String = "SolaimanLipi",
        fontSizeSp: Float = 44f,
        lineSpacingMult: Float = 1.15f,
        customCategory: String? = null,
        customText: String? = null,
        customRef: String? = null,
        customTitleColor: String? = null,
        customTextColor: String? = null,
        customRefColor: String? = null,

        autoFitText: Boolean = true,
        showLogo: Boolean = true,
        showWatermark: Boolean = true,
        textWidthPercent: Float = 1f,
        textLetterSpacing: Float = 0f,
        isTextBold: Boolean = false
    ) {
        try {
            val bitmap = generateCardBitmap(
                context = context,
                post = post,
                template = template,
                bgImageUrl = bgImageUrl,
                aspectRatio = aspectRatio,
                overlayAlpha = overlayAlpha,
                customOverlayColor = customOverlayColor,
                textAlignName = textAlignName,
                fontName = fontName,
                fontSizeSp = fontSizeSp,
                lineSpacingMult = lineSpacingMult,
                customCategory = customCategory,
                customText = customText,
                customRef = customRef,
                customTitleColor = customTitleColor,
                customTextColor = customTextColor,
                customRefColor = customRefColor,
                showLogo = showLogo,
                autoFitText = autoFitText,
                showWatermark = showWatermark,
                textWidthPercent = textWidthPercent,
                textLetterSpacing = textLetterSpacing,
                isTextBold = isTextBold
            )

            val cacheDir = File(context.cacheDir, "shared_posts")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, "post_${System.currentTimeMillis()}.png")
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

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "ফটো কার্ড শেয়ার করুন"))
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ফটো কার্ড শেয়ার করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun String?.isNullByBlank(): Boolean = this == null || this.isBlank()

    private class CustomTypefaceSpan(private val newType: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(ds: TextPaint) {
            applyCustomTypeFace(ds, newType)
        }

        override fun updateMeasureState(paint: TextPaint) {
            applyCustomTypeFace(paint, newType)
        }

        private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
            paint.typeface = tf
        }
    }

    private fun isArabicChar(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.ARABIC ||
               block == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
               block == Character.UnicodeBlock.ARABIC_EXTENDED_A ||
               block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
               block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
               c in '\u0600'..'\u06FF' || c in '\u0750'..'\u077F' || c in '\u08A0'..'\u08FF' || c in '\uFB50'..'\uFDFF' || c in '\uFE70'..'\uFEFF'
    }

    private fun formatTextWithArabicFont(text: CharSequence, shahrazadFont: Typeface): CharSequence {
        if (text.isEmpty()) return text
        var hasArabic = false
        for (i in 0 until text.length) {
            if (isArabicChar(text[i])) {
                hasArabic = true
                break
            }
        }
        if (!hasArabic) return text

        val ssb = SpannableStringBuilder(text)
        var start = -1
        for (i in 0 until ssb.length) {
            val c = ssb[i]
            val isAr = isArabicChar(c) || (start != -1 && (c == ' ' || c == 'n' || c == '۩' || c == '۝' || c in '0'..'9'))
            if (isAr) {
                if (start == -1) start = i
            } else {
                if (start != -1) {
                    var end = i
                    while (end > start && (ssb[end - 1] == ' ' || ssb[end - 1] == 'n' || ssb[end - 1] in '0'..'9')) {
                        end--
                    }
                    if (end > start) {
                        ssb.setSpan(CustomTypefaceSpan(shahrazadFont), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    start = -1
                }
            }
        }
        if (start != -1) {
            var end = ssb.length
            while (end > start && (ssb[end - 1] == ' ' || ssb[end - 1] == 'n' || ssb[end - 1] in '0'..'9')) {
                end--
            }
            if (end > start) {
                ssb.setSpan(CustomTypefaceSpan(shahrazadFont), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return ssb
    }

    suspend fun saveImageToGallery(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val filename = "QuranReader_${System.currentTimeMillis()}.jpg"
            val fos: java.io.OutputStream?
            var imageUri: android.net.Uri? = null
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/QuranReader")
                }
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES
                ).toString() + "/QuranReader"
                val file = java.io.File(imagesDir)
                if (!file.exists()) {
                    file.mkdirs()
                }
                val imageFile = java.io.File(file, filename)
                fos = java.io.FileOutputStream(imageFile)
                imageUri = android.net.Uri.fromFile(imageFile)
                
                // Add to media scanner
                android.media.MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), arrayOf("image/jpeg"), null)
            }
            
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
}

