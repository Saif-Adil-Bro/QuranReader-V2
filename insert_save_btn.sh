cat << 'INNER_EOF' > /tmp/save_btn.kt

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isSaving && cardBitmap != null) {
                                isSaving = true
                                coroutineScope.launch {
                                    val success = PostShareUtil.saveImageToGallery(context, cardBitmap!!)
                                    if (success) {
                                        Toast.makeText(context, "ছবি গ্যালারিতে সেভ হয়েছে", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "ছবি সেভ করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving && cardBitmap != null
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("গ্যালারিতে সংরক্ষণ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
INNER_EOF

sed -i -e '/\/\/ Controls Section/i\' -e "$(cat /tmp/save_btn.kt | sed 's/$/\\/')" app/src/main/java/com/example/ui/screens/PostsScreen.kt
