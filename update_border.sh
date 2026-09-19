sed -i '325,331c\
        // 3. Rounded Decorative Inner Border\
        if (template.showBorder) {\
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {\
                color = Color.parseColor(template.borderColor)\
                style = Paint.Style.STROKE\
                strokeWidth = 4f\
            }\
            canvas.drawRoundRect(RectF(32f, 32f, (width - 32).toFloat(), (finalHeight - 32).toFloat()), 28f, 28f, borderPaint)\
        }' app/src/main/java/com/example/utils/PostShareUtil.kt
