with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if "isSharing = true" in line and i == 1247:
        new_lines.append(line)
        new_lines.append("                                    coroutineScope.launch {\n")
        new_lines.append("                                        PostShareUtil.shareBitmap(context, cardBitmap!!)\n")
        new_lines.append("                                        isSharing = false\n")
        new_lines.append("                                    }\n")
        new_lines.append("                                }\n")
        new_lines.append("                            },\n")
        new_lines.append("                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),\n")
        new_lines.append("                            shape = RoundedCornerShape(10.dp),\n")
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'w') as f:
    f.writelines(new_lines)
