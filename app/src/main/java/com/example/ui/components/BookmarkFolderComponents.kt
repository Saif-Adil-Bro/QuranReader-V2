package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen

/**
 * Dialog for creating a new bookmark folder.
 */
@Composable
fun CreateBookmarkFolderDialog(
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnglish) "New Folder" else "নতুন ফোল্ডার",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = if (isEnglish)
                        "Give your bookmark folder a name"
                    else "বুকমার্ক ফোল্ডারটির একটি নাম দিন",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { if (it.length <= 40) folderName = it },
                    singleLine = true,
                    placeholder = {
                        Text(text = if (isEnglish) "e.g. Tafsir Study" else "যেমন: তাফসির পড়ার লিস্ট")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = folderName.trim()
                    if (clean.isNotEmpty()) onConfirm(clean)
                }
            ) {
                Text(
                    text = if (isEnglish) "Create" else "তৈরি করুন",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isEnglish) "Cancel" else "বাতিল",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * Dialog for renaming an existing bookmark folder.
 * Also exposes an optional delete action (shows a confirmation dialog upstream).
 */
@Composable
fun RenameBookmarkFolderDialog(
    folderName: String,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onRequestDelete: () -> Unit = {}
) {
    var newName by remember { mutableStateOf(folderName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnglish) "Rename Folder" else "ফোল্ডার পুনঃনামকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { if (it.length <= 40) newName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = newName.trim()
                    if (clean.isNotEmpty()) onConfirm(clean)
                }
            ) {
                Text(
                    text = if (isEnglish) "Save" else "সংরক্ষণ",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        },
        dismissButton = {
            Row {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = if (isEnglish) "Delete folder" else "ফোল্ডার মুছুন",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onRequestDelete() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = if (isEnglish) "Cancel" else "বাতিল",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/**
 * Dialog shown before deleting a folder.
 * Makes it explicit that the bookmarks inside are kept (moved to unfiled).
 */
@Composable
fun DeleteBookmarkFolderDialog(
    folderName: String,
    bookmarkCount: Int,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnglish) "Delete Folder?" else "ফোল্ডার মুছে ফেলবেন?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Text(
                text = if (bookmarkCount > 0) {
                    if (isEnglish)
                        "\"$folderName\" contains $bookmarkCount bookmark(s). The bookmarks will not be deleted — they will move back to the general list."
                    else
                        "\"$folderName\" ফোল্ডারে ${com.example.utils.DateUtil.toBengaliNumerals(bookmarkCount)}টি বুকমার্ক আছে। বুকমার্কগুলো মুছে যাবে না — সেগুলো আবার সাধারণ তালিকায় চলে যাবে।"
                } else {
                    if (isEnglish)
                        "\"$folderName\" is empty."
                    else "\"$folderName\" ফোল্ডারটি খালি।"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(
                    text = if (isEnglish) "Delete" else "মুছুন",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isEnglish) "Cancel" else "বাতিল",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * Lightweight folder model so the picker dialog can be shared between the
 * Quran bookmark folder system and the separate Dua bookmark folder system.
 */
data class BookmarkFolderPickerModel(
    val id: Long,
    val name: String
)

/**
 * Dialog for moving a bookmark into a folder (or back to the unfiled list),
 * and for choosing a folder when saving a NEW bookmark.
 */
@Composable
fun MoveBookmarkToFolderDialog(
    bookmarkName: String,
    folders: List<BookmarkFolderPickerModel>,
    currentFolderId: Long?,
    isNewBookmark: Boolean = false,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onMove: (Long?) -> Unit,
    onCreateFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isNewBookmark && isEnglish -> "Save to Folder"
                        isNewBookmark -> "ফোল্ডারে সংরক্ষণ করুন"
                        isEnglish -> "Move to Folder"
                        else -> "ফোল্ডারে সরান"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = bookmarkName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Unfiled option
                FolderOptionRow(
                    label = if (isEnglish) "General (no folder)" else "সাধারণ (ফোল্ডারহীন)",
                    isSelected = currentFolderId == null,
                    isEnglish = isEnglish,
                    onClick = { onMove(null) }
                )

                Spacer(modifier = Modifier.height(4.dp))
                folders.forEach { folder ->
                    FolderOptionRow(
                        label = folder.name,
                        isSelected = currentFolderId == folder.id,
                        isEnglish = isEnglish,
                        onClick = { onMove(folder.id) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Create new folder shortcut.
                // Note: only onCreateFolder() is invoked here — the caller decides
                // whether/how to dismiss this dialog first (e.g. to stash state).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreateFolder() }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "+ Create new folder" else "+ নতুন ফোল্ডার তৈরি করুন",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = when {
                        isNewBookmark && isEnglish -> "Cancel"
                        isNewBookmark -> "বাতিল"
                        isEnglish -> "Close"
                        else -> "বন্ধ করুন"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun FolderOptionRow(
    label: String,
    isSelected: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                PrimaryGreen.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Folder else Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Small colored dot used as the folder accent in folder chips.
 */
@Composable
fun FolderColorDot(colorHex: String, modifier: Modifier = Modifier) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        PrimaryGreen
    }
    Box(
        modifier = modifier
            .size(7.dp)
            .background(color, CircleShape)
    )
}
