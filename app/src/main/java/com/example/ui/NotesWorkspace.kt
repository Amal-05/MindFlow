package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.Note

@Composable
fun NotesWorkspace(
    viewModel: MainViewModel,
    notes: List<Note>,
    onAddNote: () -> Unit,
    onEditNote: (Note) -> Unit,
    onDrawNotes: () -> Unit,
    onVoiceMemo: () -> Unit = {},
    onOcrScan: () -> Unit = {}
) {
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Categories selecting filters
        CategorySelectorsBar(
            selectedTag = selectedTag,
            onTagSelected = { viewModel.setTag(it) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Personal Notebook",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onDrawNotes,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Sketch Pad", modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sketch Pad", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Voice and OCR entry shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onVoiceMemo,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text("🎙️ Voice Entry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onOcrScan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(4.dp))
                Text("📷 OCR Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (notes.isEmpty()) {
                EmptyPlaceholder(
                    icon = Icons.Default.Edit,
                    title = "Notebook list is vacant",
                    subtitle = "Draft secure reminders, meeting annotations, or select Sketch Pad to illustrate ideas using vector coordinates."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes) { note ->
                        AdvancedNoteCard(
                            note = note,
                            onClick = { onEditNote(note) },
                            onDelete = { viewModel.deleteNote(note) },
                            onTriggerAi = { viewModel.summarizeAndGenerateTasksFromNote(note) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onAddNote,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
                Spacer(Modifier.width(6.dp))
                Text("Draft Note", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvancedNoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTriggerAi: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val noteColors = getNoteCardColors(note.colorHex, isDark)
    val parsedSubtasks = note.getSubtasks()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        colors = noteColors,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (note.content.isNotEmpty()) {
                    IconButton(
                        onClick = onTriggerAi,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Gemini Spark",
                            tint = if (isDark) Color(0xFFFFD700) else Color(0xFFC5A000)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(6.dp))
            Text(
                text = note.content,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Render miniature checklist counts if present
            if (parsedSubtasks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val complete = parsedSubtasks.count { it.isChecked }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$complete/${parsedSubtasks.size} subtasks", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(noteColors.contentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(note.tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = noteColors.contentColor)
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Drop Note",
                        tint = noteColors.contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun getNoteCardColors(colorHex: String, isDark: Boolean): CardColors {
    val (bg, content) = when (colorHex) {
        "Slate" -> if (isDark) Color(0xFF1E293B) to Color(0xFFF1F5F9) else Color(0xFFF8FAFC) to Color(0xFF334155)
        "Blue" -> if (isDark) Color(0xFF1E3A8A) to Color(0xFFDBEAFE) else Color(0xFFEFF6FF) to Color(0xFF1E40AF)
        "Mint" -> if (isDark) Color(0xFF064E3B) to Color(0xFFD1FAE5) else Color(0xFFECFDF5) to Color(0xFF065F46)
        "Lavender" -> if (isDark) Color(0xFF3B0764) to Color(0xFFEDE9FE) else Color(0xFFF5F3FF) to Color(0xFF6D28D9)
        "Peach" -> if (isDark) Color(0xFF7C2D12) to Color(0xFFFFEDD5) else Color(0xFFFFF7ED) to Color(0xFFC2410C)
        "Amber" -> if (isDark) Color(0xFF78350F) to Color(0xFFFEF3C7) else Color(0xFFFFFBEB) to Color(0xFFB45309)
        else -> if (isDark) Color(0xFF1E293B) to Color(0xFFF1F5F9) else Color(0xFFF8FAFC) to Color(0xFF334155)
    }
    return CardDefaults.cardColors(containerColor = bg, contentColor = content)
}
