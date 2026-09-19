package com.example.ui.screens.mushaf.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadState
import com.example.data.model.DownloadStatus

@Composable
fun DownloadProgress(
    status: DownloadStatus,
    onPause: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { status.progress / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = Color(0xFF10B981)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${status.progress}% (${status.downloadedPages}/${status.totalPages})",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Row {
                if (status.state == DownloadState.Downloading) {
                    TextButton(onClick = onPause) {
                        Text("Pause", fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel", fontSize = 12.sp, color = Color.Red)
                }
            }
        }
    }
}
