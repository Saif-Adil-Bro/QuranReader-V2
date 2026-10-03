package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PrayerCalculationMethod

private val EmeraldPrimary = Color(0xFF00C288)
private val EmeraldDarkGreen = Color(0xFF059669)
private val DarkDialogBg = Color(0xFF131D24)
private val DarkCardSurface = Color(0xFF182228)
private val ItemDividerColor = Color(0xFF1E2F38)
private val DarkGreenText = Color(0xFF022C22)

@Composable
fun PrayerCalculationMethodDialog(
    selectedMethod: PrayerCalculationMethod,
    isEnglish: Boolean = false,
    onMethodSelected: (PrayerCalculationMethod) -> Unit,
    onDismiss: () -> Unit
) {
    // Exact list matching the calculation methods
    val methodsList = listOf(
        PrayerCalculationMethod.MWL,
        PrayerCalculationMethod.EGYPT,
        PrayerCalculationMethod.KARACHI,
        PrayerCalculationMethod.UMM_AL_QURA,
        PrayerCalculationMethod.DUBAI,
        PrayerCalculationMethod.QATAR,
        PrayerCalculationMethod.KUWAIT,
        PrayerCalculationMethod.MOONSIGHTING_COMMITTEE,
        PrayerCalculationMethod.SINGAPORE,
        PrayerCalculationMethod.ISNA
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkDialogBg,
            border = BorderStroke(1.dp, Color(0xFF1E3A2F)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Emerald Green Top Header matching the App's Islamic Green Theme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(Color(0xFF059669), Color(0xFF00C288))
                            )
                        )
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Calculation Method" else "গণনা পদ্ধতি",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF022C22),
                        textAlign = TextAlign.Center
                    )
                }

                // 2. Scrollable List of Calculation Methods
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(vertical = 6.dp)
                ) {
                    items(methodsList) { method ->
                        val isSelected = method == selectedMethod

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMethodSelected(method)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = method.getDisplayName(isEnglish),
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) EmeraldPrimary else Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val desc = if (isEnglish) method.descriptionEn else method.descriptionBn
                                    if (desc.isNotBlank()) {
                                        Text(
                                            text = desc,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF94A3B8),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onMethodSelected(method)
                                        onDismiss()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = EmeraldPrimary,
                                        unselectedColor = Color(0xFF64748B)
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            color = ItemDividerColor,
                            thickness = 0.6.dp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                // 3. Bottom Cancel / Action Pill Button in Emerald Green
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = DarkGreenText
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isEnglish) "Cancel" else "বাতিল",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
