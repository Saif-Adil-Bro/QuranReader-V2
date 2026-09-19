sed -i '281c\
        val fixedStandardHeight = when(aspectRatio) {\
            "4:5" -> 1350\
            "9:16" -> 1920\
            "16:9" -> 608\
            else -> 1080\
        }' app/src/main/java/com/example/utils/PostShareUtil.kt
