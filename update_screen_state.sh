sed -i '1106a\
    var customTitleColor by remember { mutableStateOf<String?>(null) }\
    var customTextColor by remember { mutableStateOf<String?>(null) }\
    var customRefColor by remember { mutableStateOf<String?>(null) }\
' app/src/main/java/com/example/ui/screens/PostsScreen.kt
