with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if i >= 1248 and i <= 1255:
        continue
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'w') as f:
    f.writelines(new_lines)
