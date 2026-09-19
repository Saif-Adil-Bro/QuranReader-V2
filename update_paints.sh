sed -i 's/color = Color.parseColor(template.accentColor)/color = Color.parseColor(template.titleColor)/g' app/src/main/java/com/example/utils/PostShareUtil.kt
sed -i '233c\            color = Color.parseColor(template.referenceColor)' app/src/main/java/com/example/utils/PostShareUtil.kt
