sed -i -e '/\/\/ Presets/,/            }/c\' -e "$(cat /tmp/new_ui.kt | sed 's/$/\\/')" app/src/main/java/com/example/ui/screens/PostsScreen.kt
