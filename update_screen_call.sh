sed -i 's/selectedTemplate, bgImageUrl, overlayAlpha, textAlignName, fontName, fontSizeSp, lineSpacingMult, customCategory, customText, customRef, showLogo, showWatermark, post/selectedTemplate, bgImageUrl, overlayAlpha, textAlignName, fontName, fontSizeSp, lineSpacingMult, customCategory, customText, customRef, customTitleColor, customTextColor, customRefColor, showLogo, showWatermark, post/g' app/src/main/java/com/example/ui/screens/PostsScreen.kt
sed -i '1169a\
            customTitleColor = customTitleColor,\
            customTextColor = customTextColor,\
            customRefColor = customRefColor,\
' app/src/main/java/com/example/ui/screens/PostsScreen.kt
