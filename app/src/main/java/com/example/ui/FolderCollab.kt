package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.Note

data class Folder(val name: String, val colorHex: Color, val noteTemplateCount: Int)
data class Comment(val author: String, val text: String, val timestampSecondsAgo: Int)

@Composable
fun FolderCollabScreen(
    viewModel: MainViewModel,
    notes: List<Note>,
    onSelectNote: (Note) -> Unit
) {
    var selectedFolderIndex by remember { mutableStateOf(0) }
    var showCollabDialogForNote by remember { mutableStateOf<Note?>(null) }
    var archiveMode by remember { mutableStateOf(false) }
    var trashRecoveryCount by remember { mutableStateOf(2) }

    val folders = remember {
        mutableListOf(
            Folder("All Workspaces", Color(0xFF3B82F6), 8),
            Folder("💡 Creative Ideas", Color(0xFFFBBF24), 3),
            Folder("📁 Urgent Directives", Color(0xFFEF4444), 2),
            Folder("🌸 Self Reflection Jottings", Color(0xFF10B981), 4)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Smart Folders & Cloud Engagement",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Organize notes into notebook structures and simulate group document annotations.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = {
                    archiveMode = !archiveMode
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (archiveMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
            ) {
                Icon(Icons.Default.Star, contentDescription = "Toggle Archived Display View")
            }
        }

        // Folder horizontal row selector
        Text("📁 Custom Notebook Nesting Hierarchy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            folders.forEachIndexed { i, folder ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFolderIndex == i) folder.colorHex.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFolderIndex = i }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(folder.colorHex, RoundedCornerShape(2.dp)))
                        Text(folder.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${folder.noteTemplateCount} docs", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

        // Document checklist display depending on folder
        val filteredList = if (selectedFolderIndex == 0) notes else notes.filter { n ->
            val query = folders[selectedFolderIndex].name.lowercase()
            query.contains(n.tag.lowercase()) || (selectedFolderIndex == 1 && n.tag == "Ideas") || (selectedFolderIndex == 2 && n.tag == "Urgent")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📝 Document Index (${filteredList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (trashRecoveryCount > 0) {
                TextButton(onClick = { trashRecoveryCount-- }) {
                    Text("🗑️ Recover Deleted Note ($trashRecoveryCount left)", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("This notebook is currently empty. Try adding appropriate tags inside Note editor!", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { note ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectNote(note) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                when (note.colorHex) {
                                                    "Blue" -> Color(0xFF3B82F6)
                                                    "Mint" -> Color(0xFF10B981)
                                                    "Lavender" -> Color(0xFF8B5CF6)
                                                    "Peach" -> Color(0xFFF97316)
                                                    "Amber" -> Color(0xFFFBBF24)
                                                    else -> Color.Gray
                                                },
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Text(note.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text(
                                    text = if (note.content.isNotEmpty()) note.content else "No rich content added.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1
                                )
                            }

                            // Cloud dialog trigger
                            IconButton(
                                onClick = {
                                    showCollabDialogForNote = note
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Simulate Note Collaboration",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCollabDialogForNote != null) {
        CollabDialogWidget(
            note = showCollabDialogForNote!!,
            onDismiss = { showCollabDialogForNote = null }
        )
    }
}

@Composable
fun CollabDialogWidget(note: Note, onDismiss: () -> Unit) {
    var codeInviteInput by remember { mutableStateOf("") }
    var inviteStatusText by remember { mutableStateOf("") }
    var customCommentText by remember { mutableStateOf("") }

    val comments = remember {
        mutableStateListOf(
            Comment("Sarah (Product Lead)", "I love the strategic layout here. Let's bind tasks accordingly.", 5),
            Comment("Kevin (Junior dev)", "Good idea! We should also add Pomodoro integration guidelines.", 2)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cloud Note Annotation Hub") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Collaboration on draft document: '${note.title}'",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = codeInviteInput,
                    onValueChange = { codeInviteInput = it },
                    placeholder = { Text("email@cloudworkspace.com") },
                    label = { Text("Secure User Invitation") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (inviteStatusText.isNotEmpty()) {
                    Text(inviteStatusText, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = {
                        if (codeInviteInput.isNotEmpty()) {
                            inviteStatusText = "✉️ Shared workspace invite successfully sent to $codeInviteInput"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Collaborative Access")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("💬 Shared Comments Logs", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(comments) { comment ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                Text(comment.text, fontSize = 11.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customCommentText,
                    onValueChange = { customCommentText = it },
                    placeholder = { Text("Write annotation/comment...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (customCommentText.isNotEmpty()) {
                                    comments.add(Comment("You (Owner)", customCommentText, 0))
                                    customCommentText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Post Comment")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss panel")
            }
        }
    )
}
