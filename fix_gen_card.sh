sed -i '147,158c\
    suspend fun generateCardBitmap(\
        context: Context,\
        post: ShortPost,\
        template: CardTemplate = preDefinedTemplates.first(),\
        bgImageUrl: String? = null,\
        overlayAlpha: Float = 0.70f,\
        textAlignName: String = "CENTER",\
        fontName: String = "SolaimanLipi",\
        fontSizeSp: Float = 44f,\
        lineSpacingMult: Float = 1.15f,\
        customCategory: String? = null,\
        customText: String? = null,\
        customRef: String? = null,\
        customTitleColor: String? = null,\
        customTextColor: String? = null,\
        customRefColor: String? = null,\
        showLogo: Boolean = true,\
        showWatermark: Boolean = true\
    ): Bitmap = withContext(Dispatchers.IO) {\
        val width = 1080\
        val margin = 80\
        val contentWidth = width - 2 * margin\
        val displayCategory: String = customCategory?.takeIf { it.isNotBlank() } ?: post.category\
        val displayText: String = customText?.takeIf { it.isNotBlank() } ?: post.text\
        val displayRef: String = customRef?.takeIf { it.isNotBlank() } ?: post.reference\
\
        if (bgImageUrl.isNullOrBlank() && template.id == "emerald") {\
' app/src/main/java/com/example/utils/PostShareUtil.kt
