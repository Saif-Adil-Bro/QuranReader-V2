package com.example.ui.screens.mushaf.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.data.model.DownloadState
import com.example.data.model.DownloadStatus
import com.example.data.model.MushafStyle
import com.example.util.StorageManager
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun MushafCard(
    mushaf: MushafStyle,
    downloadStatus: DownloadStatus?,
    isDefault: Boolean = false,
    onSetDefault: () -> Unit = {},
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val canRead = downloadStatus?.state == DownloadState.Downloaded || 
                              ((downloadStatus?.progress ?: 0) >= 10 && !mushaf.isPdf)
                if (canRead) {
                    onSelect()
                } else if (downloadStatus?.state != DownloadState.Downloading) {
                    onDownload()
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .padding(top = 12.dp, start = 4.dp, end = 4.dp) // Space for badges and shadows
        ) {
            // Main Cover with 3D neon border if default
            val defaultShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isDefault) {
                            Modifier
                                .shadow(
                                    elevation = 8.dp,
                                    shape = defaultShape,
                                    clip = false,
                                    ambientColor = Color(0xFF10B981),
                                    spotColor = Color(0xFF00F2FE)
                                )
                                .background(MaterialTheme.colorScheme.surface, defaultShape)
                                .border(
                                    width = 2.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF10B981), // Neon Emerald
                                            Color(0xFF00F2FE), // Neon Cyan
                                            Color(0xFF10B981)
                                        )
                                    ),
                                    shape = defaultShape
                                )
                        } else {
                            Modifier
                                .shadow(
                                    elevation = 3.dp,
                                    shape = defaultShape,
                                    clip = false
                                )
                                .background(MaterialTheme.colorScheme.surface, defaultShape)
                                .border(1.dp, Color(0xFFE5E7EB), defaultShape)
                        }
                    )
                    .clip(defaultShape)
            ) {
                val context = LocalContext.current
                val storageManager = remember(context) { StorageManager(context) }
                val localPageFile = remember(mushaf.id, downloadStatus?.state) {
                    val page2 = storageManager.getPageFile(mushaf.id, 2)
                    val page1 = storageManager.getPageFile(mushaf.id, 1)
                    when {
                        page2.exists() -> page2
                        page1.exists() -> page1
                        else -> null
                    }
                }
                val cardImageModel: Any = localPageFile ?: mushaf.thumbnailUrl
                val cardImageRequest = remember(cardImageModel) {
                    ImageRequest.Builder(context)
                        .data(cardImageModel)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = cardImageRequest,
                    contentDescription = "Page Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(defaultShape),
                    loading = {
                        OfflineMushafCover(mushaf = mushaf)
                    },
                    error = {
                        OfflineMushafCover(mushaf = mushaf)
                    }
                )
                
                if (downloadStatus?.state == DownloadState.Downloading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                progress = { (downloadStatus.progress / 100f) },
                                color = Color(0xFF10B981),
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${downloadStatus.progress}%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Premium Status Badge (Online / Offline) at the top-left of the cover
                if (downloadStatus?.state == DownloadState.Downloaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .shadow(3.dp, RoundedCornerShape(6.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Offline / Saved",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "অফলাইন",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (downloadStatus?.state != DownloadState.Downloading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .shadow(3.dp, RoundedCornerShape(6.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7))
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Online / Downloadable",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "অনলাইন",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Checkmark Badge for default active Mushaf (at top center)
            if (isDefault) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp)
                        .size(24.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Default active Mushaf",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mushaf.nameBengali,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mushaf.descriptionBengali,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (downloadStatus?.state != DownloadState.Downloading && downloadStatus?.state != DownloadState.Downloaded) {
                        DropdownMenuItem(
                            text = { Text("ডাউনলোড করুন") },
                            onClick = { 
                                showMenu = false
                                onDownload()
                            }
                        )
                    }
                    if (downloadStatus?.state == DownloadState.Downloading) {
                        DropdownMenuItem(
                            text = { Text("পজ করুন") },
                            onClick = { 
                                showMenu = false
                                onPause()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("বাতিল করুন") },
                            onClick = { 
                                showMenu = false
                                onCancel()
                            }
                        )
                    }
                    if (downloadStatus?.state == DownloadState.Downloaded || ((downloadStatus?.progress ?: 0) >= 10 && !mushaf.isPdf)) {
                        DropdownMenuItem(
                            text = { Text("পড়ুন") },
                            onClick = { 
                                showMenu = false
                                onSelect()
                            }
                        )
                    }
                    if (!isDefault && downloadStatus?.state == DownloadState.Downloaded) {
                        DropdownMenuItem(
                            text = { Text("ডিফল্ট সেট করুন") },
                            onClick = { 
                                showMenu = false
                                onSetDefault()
                            }
                        )
                    }
                    if ((downloadStatus?.downloadedPages ?: 0) > 0) {
                        DropdownMenuItem(
                            text = { Text("মুছে ফেলুন", color = Color.Red) },
                            onClick = { 
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun MushafBookCoverPreview(
    mushaf: MushafStyle,
    isDownloaded: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFAF6EB)) // Soft cream page background color
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        val context = LocalContext.current
        val storageManager = remember(context) { StorageManager(context) }
        val localPageFile = remember(mushaf.id, isDownloaded) {
            val page2 = storageManager.getPageFile(mushaf.id, 2)
            val page1 = storageManager.getPageFile(mushaf.id, 1)
            when {
                page2.exists() -> page2
                page1.exists() -> page1
                else -> null
            }
        }
        val previewImageModel: Any = localPageFile ?: mushaf.thumbnailUrl
        val previewImageRequest = remember(previewImageModel) {
            ImageRequest.Builder(context)
                .data(previewImageModel)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }
        SubcomposeAsyncImage(
            model = previewImageRequest,
            contentDescription = "Page Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                OfflineMushafCover(mushaf = mushaf)
            },
            error = {
                OfflineMushafCover(mushaf = mushaf)
            }
        )

        if (isDownloaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "সংরক্ষিত",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OfflineMushafCover(
    mushaf: MushafStyle,
    modifier: Modifier = Modifier
) {
    val (bgGradients, accentColor) = remember(mushaf.id) {
        when (mushaf.id) {
            "imdadia_hafezi" -> listOf(Color(0xFF064E3B), Color(0xFF042F2E), Color(0xFF022C22)) to Color(0xFFF59E0B)
            "hafizi_15line" -> listOf(Color(0xFF1E3A8A), Color(0xFF1E1B4B), Color(0xFF0F172A)) to Color(0xFF38BDF8)
            "indopak" -> listOf(Color(0xFF701A75), Color(0xFF4C0519), Color(0xFF2E1065)) to Color(0xFFF472B6)
            else -> listOf(Color(0xFF064E3B), Color(0xFF042F2E), Color(0xFF022C22)) to Color(0xFF10B981)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradients)),
        contentAlignment = Alignment.Center
    ) {
        // Gold / Decorative frame border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(4.dp)
                .border(0.8.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(10.dp)
        ) {
            // Emblem symbol
            IslamicEmblem(
                modifier = Modifier.size(40.dp),
                color = accentColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "القرآن الكريم",
                color = accentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = mushaf.nameBengali,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = accentColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, accentColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = if (mushaf.isPdf) "অফলাইন পিডিএফ" else "১৫ লাইন",
                    color = accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun IslamicEmblem(modifier: Modifier = Modifier, color: Color) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer rotated square (8-pointed star shape base)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, color, RoundedCornerShape(6.dp))
                .rotate(45f)
                .border(1.dp, color, RoundedCornerShape(6.dp))
        )
        // Center text symbol
        Text(
            text = "القرآن",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
