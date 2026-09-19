sed -i '1628a\
                        // 5. Text Color Settings\
                        Column {\
                            Text(\
                                text = "🎨 টেক্সট কালার (কাস্টম)",\
                                fontSize = 14.sp,\
                                fontWeight = FontWeight.Bold,\
                                color = MaterialTheme.colorScheme.onSurface\
                            )\
                            Spacer(modifier = Modifier.height(8.dp))\
                            Text("টাইটেল/ক্যাটাগরি কালার:", fontSize = 12.sp, color = Color.Gray)\
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\
                                item {\
                                    FilterChip(\
                                        selected = customTitleColor == null,\
                                        onClick = { customTitleColor = null },\
                                        label = { Text("টেমপ্লেট ডিফল্ট", fontSize = 12.sp) }\
                                    )\
                                }\
                                items(PostShareUtil.TextColorPresets) { preset ->\
                                    FilterChip(\
                                        selected = customTitleColor == preset.hex,\
                                        onClick = { customTitleColor = preset.hex },\
                                        label = { Text(preset.name, fontSize = 12.sp) },\
                                        colors = FilterChipDefaults.filterChipColors(\
                                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),\
                                            selectedLabelColor = PrimaryGreen\
                                        )\
                                    )\
                                }\
                            }\
                            Spacer(modifier = Modifier.height(4.dp))\
                            Text("বডি টেক্সট কালার:", fontSize = 12.sp, color = Color.Gray)\
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\
                                item {\
                                    FilterChip(\
                                        selected = customTextColor == null,\
                                        onClick = { customTextColor = null },\
                                        label = { Text("টেমপ্লেট ডিফল্ট", fontSize = 12.sp) }\
                                    )\
                                }\
                                items(PostShareUtil.TextColorPresets) { preset ->\
                                    FilterChip(\
                                        selected = customTextColor == preset.hex,\
                                        onClick = { customTextColor = preset.hex },\
                                        label = { Text(preset.name, fontSize = 12.sp) },\
                                        colors = FilterChipDefaults.filterChipColors(\
                                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),\
                                            selectedLabelColor = PrimaryGreen\
                                        )\
                                    )\
                                }\
                            }\
                            Spacer(modifier = Modifier.height(4.dp))\
                            Text("রেফারেন্স কালার:", fontSize = 12.sp, color = Color.Gray)\
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\
                                item {\
                                    FilterChip(\
                                        selected = customRefColor == null,\
                                        onClick = { customRefColor = null },\
                                        label = { Text("টেমপ্লেট ডিফল্ট", fontSize = 12.sp) }\
                                    )\
                                }\
                                items(PostShareUtil.TextColorPresets) { preset ->\
                                    FilterChip(\
                                        selected = customRefColor == preset.hex,\
                                        onClick = { customRefColor = preset.hex },\
                                        label = { Text(preset.name, fontSize = 12.sp) },\
                                        colors = FilterChipDefaults.filterChipColors(\
                                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),\
                                            selectedLabelColor = PrimaryGreen\
                                        )\
                                    )\
                                }\
                            }\
                        }\
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))\
' app/src/main/java/com/example/ui/screens/PostsScreen.kt
