sed -i '324c\
                    decoded\
                }\
                if (loadedBg != null && !loadedBg.isRecycled) {' app/src/main/java/com/example/utils/PostShareUtil.kt
sed -i '344d' app/src/main/java/com/example/utils/PostShareUtil.kt # remove duplicate dstRect
