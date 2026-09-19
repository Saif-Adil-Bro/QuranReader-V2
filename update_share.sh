cat << 'INNER_EOF' > /tmp/update_share.kt
                        Button(
                            onClick = {
                                if (!isSharing && cardBitmap != null) {
                                    isSharing = true
                                    coroutineScope.launch {
                                        PostShareUtil.shareBitmap(context, cardBitmap!!)
                                        isSharing = false
                                    }
                                }
                            },
INNER_EOF

# The existing share logic calls PostShareUtil.shareAsImage(...) which generates the bitmap AGAIN.
# But we already have cardBitmap generated in the UI (for preview and saving).
# To make it efficient and follow "render/export before share" we should share the existing cardBitmap.
# We will create a shareBitmap function in PostShareUtil.

cat << 'INNER_EOF' > /tmp/share_bitmap.kt
    suspend fun shareBitmap(context: Context, bitmap: Bitmap) {
        try {
            val cacheDir = File(context.cacheDir, "shared_posts")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, "post_${System.currentTimeMillis()}.png")
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "ফটো কার্ড শেয়ার করুন"))
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ফটো কার্ড শেয়ার করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
INNER_EOF

sed -i -e '/suspend fun shareAsImage(/i\' -e "$(cat /tmp/share_bitmap.kt | sed 's/$/\\/')" app/src/main/java/com/example/utils/PostShareUtil.kt

# Now update PostsScreen.kt to use shareBitmap instead of shareAsImage with all parameters.
sed -i -e '/if (!isSharing) {/,/isSharing = false/c\' -e "                                if (!isSharing && cardBitmap != null) {\\
                                    isSharing = true\\
                                    coroutineScope.launch {\\
                                        PostShareUtil.shareBitmap(context, cardBitmap!!)\\
                                        isSharing = false\\
                                    }\\
                                }" app/src/main/java/com/example/ui/screens/PostsScreen.kt

