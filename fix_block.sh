sed -i '324,346c\
                    decoded\
                }\
                if (loadedBg != null && !loadedBg.isRecycled) {\
                    val imgRatio = loadedBg.width.toFloat() / loadedBg.height.toFloat()\
                    val canvasRatio = width.toFloat() / finalHeight.toFloat()\
\
                    var srcX = 0\
                    var srcY = 0\
                    var srcW = loadedBg.width\
                    var srcH = loadedBg.height\
\
                    if (imgRatio > canvasRatio) {\
                        srcW = (loadedBg.height * canvasRatio).toInt()\
                        srcX = (loadedBg.width - srcW) / 2\
                    } else {\
                        srcH = (loadedBg.width / canvasRatio).toInt()\
                        srcY = (loadedBg.height - srcH) / 2\
                    }\
\
                    val srcRect = Rect(srcX, srcY, srcX + srcW, srcY + srcH)\
                    val dstRect = Rect(0, 0, width, finalHeight)\
                    canvas.drawBitmap(loadedBg, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))\
                    bgBitmapDrawn = true\
                }' app/src/main/java/com/example/utils/PostShareUtil.kt
