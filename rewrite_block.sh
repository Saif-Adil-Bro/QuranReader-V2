sed -i '1750,1771c\
                            // Font Size\
                            Column {\
                                Row(\
                                    modifier = Modifier.fillMaxWidth(),\
                                    horizontalArrangement = Arrangement.SpaceBetween,\
                                    verticalAlignment = Alignment.CenterVertically\
                                ) {\
                                    Text(\
                                        text = "🔠 ফন্ট সাইজ",\
                                        fontSize = 14.sp,\
                                        fontWeight = FontWeight.Bold,\
                                        color = MaterialTheme.colorScheme.onSurface\
                                    )\
                                    Text(\
                                        text = "${fontSizeSp.toInt()} sp",\
                                        fontSize = 13.sp,\
                                        fontWeight = FontWeight.Bold,\
                                        color = PrimaryGreen\
                                    )\
                                }\
                                Spacer(modifier = Modifier.height(4.dp))\
                                Slider(\
                                    value = fontSizeSp,\
                                    onValueChange = { fontSizeSp = it },\
                                    valueRange = 32f..58f,\
                                    colors = SliderDefaults.colors(thumbColor = PrimaryGreen, activeTrackColor = PrimaryGreen)\
                                )\
                                Spacer(modifier = Modifier.height(4.dp))\
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {\
                                    Text("অটো-ফিট (Auto Fit)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)\
                                    Switch(checked = autoFitText, onCheckedChange = { autoFitText = it }, colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen, checkedTrackColor = PrimaryGreen.copy(alpha = 0.5f)))\
                                }\
                            }\
' app/src/main/java/com/example/ui/screens/PostsScreen.kt
