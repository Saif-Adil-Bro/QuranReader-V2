with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'r') as f:
    content = f.read()

import re
pattern = r"onClick = {\s*if \(\!isSharing && cardBitmap \!= null\) {\s*isSharing = true\s*coroutineScope\.launch {\s*PostShareUtil\.shareBitmap\(context, cardBitmap\!\!\)\s*isSharing = false\s*}\s*},\s*}\s*}\s*}\s*shape = RoundedCornerShape\(10\.dp\),"

replacement = """onClick = {
                                if (!isSharing && cardBitmap != null) {
                                    isSharing = true
                                    coroutineScope.launch {
                                        PostShareUtil.shareBitmap(context, cardBitmap!!)
                                        isSharing = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(10.dp),"""

content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'w') as f:
    f.write(content)
