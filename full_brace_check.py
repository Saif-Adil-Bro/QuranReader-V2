with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt') as f:
    lines = f.readlines()

count = 0
for i, l in enumerate(lines):
    count += l.count('{')
    count -= l.count('}')
    if count < 0:
        print(f"Extra closing brace at line {i+1}")
        count = 0

print(f"Final count: {count}")
