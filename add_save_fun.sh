cat << 'INNER_EOF' > /tmp/save_fun.kt

    suspend fun saveImageToGallery(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val filename = "QuranReader_${System.currentTimeMillis()}.jpg"
            val fos: java.io.OutputStream?
            var imageUri: android.net.Uri? = null
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/QuranReader")
                }
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES
                ).toString() + "/QuranReader"
                val file = java.io.File(imagesDir)
                if (!file.exists()) {
                    file.mkdirs()
                }
                val imageFile = java.io.File(file, filename)
                fos = java.io.FileOutputStream(imageFile)
                imageUri = android.net.Uri.fromFile(imageFile)
                
                // Add to media scanner
                android.media.MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), arrayOf("image/jpeg"), null)
            }
            
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
INNER_EOF

# Insert into PostShareUtil.kt before the closing brace of the object PostShareUtil
sed -i -e '/^}$/i\' -e "$(cat /tmp/save_fun.kt | sed 's/$/\\/')" app/src/main/java/com/example/utils/PostShareUtil.kt
