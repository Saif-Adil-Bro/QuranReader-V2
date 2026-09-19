cat << 'INNER_EOF' > /tmp/new_presets.kt
    val bgCategories = listOf("সব", "মসজিদ", "প্রকৃতি", "রাত", "কুরআন", "Abstract", "রমজান", "ইসলামিক")
    var selectedBgCategory by remember { mutableStateOf("সব") }
    
    data class PresetBg(val url: String, val category: String)
    val presetBgList = remember {
        listOf(
            PresetBg("https://images.unsplash.com/photo-1542816417-0983cbe33577?w=600&q=80", "মসজিদ"),
            PresetBg("https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&q=80", "মসজিদ"),
            PresetBg("https://images.unsplash.com/photo-1519817650390-64a93db51149?w=600&q=80", "মসজিদ"),
            PresetBg("https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?w=600&q=80", "মসজিদ"),
            PresetBg("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=80", "প্রকৃতি"),
            PresetBg("https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=600&q=80", "প্রকৃতি"),
            PresetBg("https://images.unsplash.com/photo-1444464666168-49b626d49c97?w=600&q=80", "প্রকৃতি"),
            PresetBg("https://images.unsplash.com/photo-1426604966848-d7adac402bff?w=600&q=80", "প্রকৃতি"),
            PresetBg("https://images.unsplash.com/photo-1505322022379-7c3353ee6291?w=600&q=80", "রাত"),
            PresetBg("https://images.unsplash.com/photo-1488866022504-f2584929ca5f?w=600&q=80", "রাত"),
            PresetBg("https://images.unsplash.com/photo-1503264116251-35a269479413?w=600&q=80", "রাত"),
            PresetBg("https://images.unsplash.com/photo-1609599006353-e629aaab31f5?w=600&q=80", "কুরআন"),
            PresetBg("https://images.unsplash.com/photo-1576485290814-1c72aa4bbb8e?w=600&q=80", "কুরআন"),
            PresetBg("https://images.unsplash.com/photo-1509021436468-d51030005963?w=600&q=80", "Abstract"),
            PresetBg("https://images.unsplash.com/photo-1604871000636-074fa5117945?w=600&q=80", "Abstract"),
            PresetBg("https://images.unsplash.com/photo-1557672172-298e090bd0f1?w=600&q=80", "Abstract"),
            PresetBg("https://images.unsplash.com/photo-1585036156171-384164a8c675?w=600&q=80", "রমজান"),
            PresetBg("https://images.unsplash.com/photo-1555068228-4b71ab2ab1b5?w=600&q=80", "রমজান"),
            PresetBg("https://images.unsplash.com/photo-1563852028710-184518485244?w=600&q=80", "ইসলামিক"),
            PresetBg("https://images.unsplash.com/photo-1582281171801-62a26563deec?w=600&q=80", "ইসলামিক"),
            PresetBg("https://images.unsplash.com/photo-1558231908-1647ecb6243b?w=600&q=80", "ইসলামিক")
        )
    }
INNER_EOF

# Now we need to inject this into PostsScreen.kt and replace the old presetBgUrls definition.
# The old definition is at line 1164 to 1172:
#     val presetBgUrls = remember {
#         listOf(
#             "https://images.unsplash.com/photo-1542816417-0983cbe33577?w=800&q=80" to "মসজিদ ১",
#             "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=800&q=80" to "মসজিদ ২",
#             "https://images.unsplash.com/photo-1519817650390-64a93db51149?w=800&q=80" to "তারা ও আকাশ",
#             "https://images.unsplash.com/photo-1509021436468-d51030005963?w=800&q=80" to "জ্যামিতিক প্যাটার্ন",
#             "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80" to "প্রকৃতি"
#         )
#     }

sed -i -e '/val presetBgUrls = remember {/,/}/c\' -e "$(cat /tmp/new_presets.kt | sed 's/$/\\/')" app/src/main/java/com/example/ui/screens/PostsScreen.kt

