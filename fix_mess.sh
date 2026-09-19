sed -i '1502,1504c\
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))\
                        // Aspect Ratio Selection\
                        Column {\
                            Text(\
                                text = "📏 কার্ডের সাইজ (Aspect Ratio)",\
                                fontSize = 14.sp,\
                                fontWeight = FontWeight.Bold,\
                                color = MaterialTheme.colorScheme.onSurface\
                            )\
                            Spacer(modifier = Modifier.height(8.dp))\
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\
                                val ratios = listOf("1:1" to "1:1", "4:5" to "4:5", "9:16" to "9:16", "16:9" to "16:9")\
                                ratios.forEach { (label, value) ->\
                                    val isSelected = (aspectRatio == value)\
                                    FilterChip(\
                                        selected = isSelected,\
                                        onClick = { aspectRatio = value },\
                                        label = { Text(label, fontSize = 12.sp) },\
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f), selectedLabelColor = PrimaryGreen)\
                                    )\
                                }\
                            }\
                        }\
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))\
                        // 3. Bangla Fonts Selection\
                        Column {' app/src/main/java/com/example/ui/screens/PostsScreen.kt
