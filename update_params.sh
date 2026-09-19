sed -i '153i\        aspectRatio: String = "1:1",' app/src/main/java/com/example/utils/PostShareUtil.kt
sed -i '434i\        aspectRatio: String = "1:1",' app/src/main/java/com/example/utils/PostShareUtil.kt
sed -i '/bgImageUrl = bgImageUrl,/a \                aspectRatio = aspectRatio,' app/src/main/java/com/example/utils/PostShareUtil.kt
