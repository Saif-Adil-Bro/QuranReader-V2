sed -i '68c\
    data class ColorPreset(val name: String, val hex: String)\
    val TextColorPresets = listOf(\
        ColorPreset("White", "#FFFFFF"),\
        ColorPreset("Cream", "#FEF3C7"),\
        ColorPreset("Gold", "#FBBF24"),\
        ColorPreset("Emerald", "#10B981"),\
        ColorPreset("Dark Green", "#064E3B"),\
        ColorPreset("Black", "#000000"),\
        ColorPreset("Light Gray", "#E2E8F0")\
    )\
' app/src/main/java/com/example/utils/PostShareUtil.kt
