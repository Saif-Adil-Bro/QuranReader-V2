sed -i '1466,1488c\
                            if (bgImageUrl.isNotEmpty()) {\
                                Spacer(modifier = Modifier.height(16.dp))\
                                Text("ওভারলে ইনটেনসিটি (Overlay Intensity):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)\
                                Spacer(modifier = Modifier.height(8.dp))\
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\
                                    val intensities = listOf("None" to 0.0f, "Light" to 0.3f, "Medium" to 0.6f, "Dark" to 0.85f)\
                                    intensities.forEach { (label, alphaValue) ->\
                                        val isSelected = (overlayAlpha == alphaValue)\
                                        FilterChip(\
                                            selected = isSelected,\
                                            onClick = { overlayAlpha = alphaValue },\
                                            label = { Text(label, fontSize = 12.sp) },\
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f), selectedLabelColor = PrimaryGreen)\
                                        )\
                                    }\
                                }\
\
                                Spacer(modifier = Modifier.height(12.dp))\
                                Text("ওভারলে কালার (Overlay Color):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)\
                                Spacer(modifier = Modifier.height(8.dp))\
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\
                                    val colors = listOf("Black" to "#000000", "White" to "#FFFFFF", "Dark Green" to "#064E3B")\
                                    colors.forEach { (label, hex) ->\
                                        val isSelected = (customOverlayColor == hex)\
                                        FilterChip(\
                                            selected = isSelected,\
                                            onClick = { customOverlayColor = hex },\
                                            label = { Text(label, fontSize = 12.sp) },\
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f), selectedLabelColor = PrimaryGreen)\
                                        )\
                                    }\
                                }\
                            }' app/src/main/java/com/example/ui/screens/PostsScreen.kt
