sed -i '80,95c\
    data class CardTemplate(\
        val id: String,\
        val title: String,\
        val categories: List<TemplateCategory>,\
        val bgColors: Pair<String, String>,\
        val textColor: String,\
        val titleColor: String,\
        val referenceColor: String,\
        val borderColor: String,\
        val showBorder: Boolean = true,\
        val defaultTextAlign: String = "CENTER",\
        val defaultFontName: String = "SolaimanLipi",\
        val defaultFontSize: Float = 44f,\
        val defaultLineSpacing: Float = 1.15f,\
        val defaultOverlayAlpha: Float = 0.70f,\
        val showLogo: Boolean = true,\
        val showWatermark: Boolean = true\
    )' app/src/main/java/com/example/utils/PostShareUtil.kt
