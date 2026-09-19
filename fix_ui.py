import re

with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'r') as f:
    content = f.read()

replacement = """// Background Preset Categories
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(bgCategories) { category ->
                                    FilterChip(
                                        selected = selectedBgCategory == category,
                                        onClick = { selectedBgCategory = category },
                                        label = { Text(category) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f), selectedLabelColor = PrimaryGreen)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Background Image Grid
                            val filteredBgs = presetBgList.filter { selectedBgCategory == "সব" || it.category == selectedBgCategory }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (selectedBgCategory == "সব") {
                                    OutlinedButton(onClick = { bgImageUrl = "" }, modifier = Modifier.fillMaxWidth()) {
                                        Text("কোনো ছবি নয় (Clear Background)", color = PrimaryGreen)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                filteredBgs.chunked(3).forEach { rowItems ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowItems.forEach { bg ->
                                            val isSelected = bgImageUrl == bg.url
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) PrimaryGreen else androidx.compose.ui.graphics.Color.LightGray,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { bgImageUrl = bg.url }
                                            ) {
                                                coil.compose.AsyncImage(
                                                    model = bg.url,
                                                    contentDescription = "Background",
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        val emptySpots = 3 - rowItems.size
                                        for (i in 0 until emptySpots) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }"""

# Find the block to replace
pattern = r"// Presets\s+LazyRow.*?}\s+items\(presetBgUrls\).*?}\s+}"
content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/PostsScreen.kt', 'w') as f:
    f.write(content)
