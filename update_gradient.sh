sed -i '338,343c\
        val topOverlayColor = customOverlayColor?.let { Color.parseColor(it) } ?: Color.parseColor(template.bgColors.first)\
        val bottomOverlayColor = customOverlayColor?.let { Color.parseColor(it) } ?: Color.parseColor(template.bgColors.second)\
\
        val shader = LinearGradient(\
            0f, 0f, 0f, finalHeight.toFloat(),\
            topOverlayColor,\
            bottomOverlayColor,\
            Shader.TileMode.CLAMP\
        )' app/src/main/java/com/example/utils/PostShareUtil.kt
