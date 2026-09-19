sed -i '266,286c\
        var currentFontSize = fontSizeSp\
        val minFontSize = 20f\
        var textLayout: StaticLayout\
        var textHeight: Float\
        val categoryText = if (displayCategory.isNotBlank()) "— $displayCategory —" else ""\
        val refText = if (displayRef.isNotEmpty()) "— $displayRef —" else ""\
        val creditText = "📱 ❝কুরআন রিডার❞ অ্যাপ থেকে সংগৃহীত"\
        val categoryHeight = if (categoryText.isNotEmpty()) 50f else 0f\
        val gap1 = if (categoryText.isNotEmpty()) 30f else 0f\
        val gap2 = if (refText.isNotEmpty()) 30f else 0f\
        val refHeight = if (refText.isNotEmpty()) 45f else 0f\
        val topHeaderSpace = 130f  // Reserved at top for border and top-left credit logo\
        val bottomFooterSpace = 130f // Reserved at bottom for divider line and credit watermark\
        val fixedStandardHeight = 1080\
        val maxMiddleHeight = fixedStandardHeight - topHeaderSpace - bottomFooterSpace\
\
        while (true) {\
            textPaint.textSize = currentFontSize\
            textLayout = StaticLayout.Builder.obtain(\
                formattedDisplayText, 0, formattedDisplayText.length, textPaint, contentWidth\
            ).setAlignment(staticLayoutAlign).setLineSpacing(12f, lineSpacingMult).build()\
            \
            textHeight = textLayout.height.toFloat()\
            val currentMiddleHeight = categoryHeight + gap1 + textHeight + gap2 + refHeight\
            \
            if (autoFitText && currentMiddleHeight > maxMiddleHeight && currentFontSize > minFontSize) {\
                currentFontSize -= 2f\
            } else {\
                break\
            }\
        }\
\
        val middleContentHeight = categoryHeight + gap1 + textHeight + gap2 + refHeight\
        val requiredHeight = (topHeaderSpace + middleContentHeight + bottomFooterSpace).toInt()\
        val finalHeight = requiredHeight.coerceAtLeast(fixedStandardHeight)' app/src/main/java/com/example/utils/PostShareUtil.kt
