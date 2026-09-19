sed -i 's/Color.parseColor(template.titleColor)/Color.parseColor(customTitleColor ?: template.titleColor)/g' app/src/main/java/com/example/utils/PostShareUtil.kt
sed -i 's/Color.parseColor(template.textColor)/Color.parseColor(customTextColor ?: template.textColor)/g' app/src/main/java/com/example/utils/PostShareUtil.kt
sed -i 's/Color.parseColor(template.referenceColor)/Color.parseColor(customRefColor ?: template.referenceColor)/g' app/src/main/java/com/example/utils/PostShareUtil.kt
