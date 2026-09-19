sed -i '1175c\
        selectedTemplate, bgImageUrl, overlayAlpha, customOverlayColor, aspectRatio, textAlignName, fontName, fontSizeSp, lineSpacingMult, customCategory, customText, customRef, customTitleColor, customTextColor, customRefColor, showLogo, showWatermark, autoFitText, post' app/src/main/java/com/example/ui/screens/PostsScreen.kt
sed -i '/bgImageUrl = bgImageUrl.ifBlank { null },/a \            aspectRatio = aspectRatio,' app/src/main/java/com/example/ui/screens/PostsScreen.kt
