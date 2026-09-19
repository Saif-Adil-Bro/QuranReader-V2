sed -i '1270,1350c\
                        if (cardBitmap != null) {\
                            Image(\
                                bitmap = cardBitmap!!.asImageBitmap(),\
                                contentDescription = "Card Preview",\
                                modifier = Modifier\
                                    .fillMaxSize()\
                                    .padding(8.dp)\
                            )\
                        }\
\
                        if (isGeneratingPreview && cardBitmap == null) {\
                            Box(\
                                modifier = Modifier\
                                    .fillMaxSize()\
                                    .background(Color.Black.copy(alpha = 0.3f)),\
                                contentAlignment = Alignment.Center\
                            ) {\
                                CircularProgressIndicator(color = PrimaryGreen)\
                            }\
                        }\
                    }\
                }\
\
                // Controls Section\
                Card(\
                    modifier = Modifier.fillMaxWidth(),\
                    shape = RoundedCornerShape(16.dp),\
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)\
                ) {\
                    Column(\
                        modifier = Modifier\
                            .fillMaxWidth()\
                            .padding(16.dp),\
                        verticalArrangement = Arrangement.spacedBy(16.dp)\
                    ) {\
                        // 1. Template Presets & Categories\
                        Column {\
                            Text(\
                                text = "🎨 টেমপ্লেট ও স্টাইল",\
                                fontSize = 14.sp,\
                                fontWeight = FontWeight.Bold,\
                                color = MaterialTheme.colorScheme.onSurface\
                            )\
                            Spacer(modifier = Modifier.height(8.dp))\
                            \
                            // Category Selector\
                            LazyRow(\
                                horizontalArrangement = Arrangement.spacedBy(8.dp),\
                                modifier = Modifier.fillMaxWidth()\
                            ) {\
                                items(PostShareUtil.TemplateCategory.entries.toTypedArray()) { category ->\
                                    FilterChip(\
                                        selected = selectedCategory == category,\
                                        onClick = { selectedCategory = category },\
                                        label = { Text(category.title, fontSize = 12.sp) },\
                                        colors = FilterChipDefaults.filterChipColors(\
                                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),\
                                            selectedLabelColor = PrimaryGreen\
                                        )\
                                    )\
                                }\
                            }\
                            \
                            Spacer(modifier = Modifier.height(12.dp))\
                            \
                            // Templates Selector\
                            LazyRow(\
                                horizontalArrangement = Arrangement.spacedBy(8.dp),\
                                modifier = Modifier.fillMaxWidth()\
                            ) {\
                                val filteredTemplates = PostShareUtil.preDefinedTemplates.filter { selectedCategory == PostShareUtil.TemplateCategory.ALL || selectedCategory in it.categories }\
                                items(filteredTemplates) { template ->\
                                    val isSelected = selectedTemplate == template\
                                    val themeBg = Color(android.graphics.Color.parseColor(template.bgColors.first))\
                                    Box(\
                                        modifier = Modifier\
                                            .clip(RoundedCornerShape(12.dp))\
                                            .background(themeBg)\
                                            .border(\
                                                width = if (isSelected) 3.dp else 1.dp,\
                                                color = if (isSelected) PrimaryGreen else Color.Gray.copy(alpha = 0.4f),\
                                                shape = RoundedCornerShape(12.dp)\
                                            )\
                                            .clickable { selectedTemplate = template }\
                                            .padding(horizontal = 14.dp, vertical = 10.dp)\
                                    ) {\
                                        Text(\
                                            text = template.title,\
                                            fontSize = 12.sp,\
                                            color = Color(android.graphics.Color.parseColor(template.textColor)),\
                                            fontWeight = FontWeight.Bold\
                                        )\
                                    }\
                                }\
                            }\
                        }' app/src/main/java/com/example/ui/screens/PostsScreen.kt
