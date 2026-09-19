with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "isSharing = false" in line and "shareBitmap" in lines[i-1]:
        new_lines.append(line)
        new_lines.append("                                }\n")
        new_lines.append("                            },\n")
        # skip next few lines
    elif "if (!isSharing && cardBitmap != null) {" in line:
        new_lines.append(line)
    elif i >= 1253 and i <= 1256:
        # the corrupted lines
        continue
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'w') as f:
    f.writelines(new_lines)
