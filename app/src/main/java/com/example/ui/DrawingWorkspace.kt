package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel

data class LineStroke(val path: Path, val color: Color, val strokeWidth: Float)

@Composable
fun DrawingWorkspace(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var sketchName by remember { mutableStateOf("Design Plan Blueprint") }
    val paths = remember { mutableStateListOf<LineStroke>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedBrushWidth by remember { mutableStateOf(8f) }
    var isEraserMode by remember { mutableStateOf(false) }
    var showPaperGrid by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Interactive Vector Drawing Pad", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Express design ideas, graphs, or recall vectors directly via sketch strokes.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = sketchName,
                onValueChange = { sketchName = it },
                label = { Text("Canvas Project Label") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Paper Grid indicator toggle
            IconButton(
                onClick = { showPaperGrid = !showPaperGrid },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (showPaperGrid) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Toggle graph paper grid")
            }
        }

        // Palette selector & Brush controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colors row
            val colors = listOf(Color.Black, Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFFBBF24), Color(0xFFA78BFA))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                colors.forEach { col ->
                    val isSel = selectedColor == col && !isEraserMode
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(
                                width = if (isSel) 3.dp else 0.dp,
                                color = if (isSel) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColor = col
                                isEraserMode = false
                            }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Eraser mode selection
                IconButton(
                    onClick = { isEraserMode = !isEraserMode },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isEraserMode) MaterialTheme.colorScheme.errorContainer else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Use Eraser brush", tint = if (isEraserMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Brush widths adjustments
                Text("Size:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                listOf(4f, 8f, 16f, 24f).forEach { width ->
                    val isWidthSel = selectedBrushWidth == width
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isWidthSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedBrushWidth = width }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((width / 4f + 3f).dp)
                                .clip(CircleShape)
                                .background(if (isWidthSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        // Drawing Canvas whiteboard component
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .pointerInput(selectedColor, selectedBrushWidth, isEraserMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val path = Path().apply { moveTo(offset.x, offset.y) }
                            currentPath = path
                            val col = if (isEraserMode) Color.White else selectedColor
                            paths.add(LineStroke(path, col, selectedBrushWidth))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath?.lineTo(change.position.x, change.position.y)
                            if (currentPath != null && paths.isNotEmpty()) {
                                val idx = paths.size - 1
                                val col = if (isEraserMode) Color.White else selectedColor
                                paths[idx] = LineStroke(currentPath!!, col, selectedBrushWidth)
                            }
                        },
                        onDragEnd = {
                            currentPath = null
                        }
                    )
                }
        ) {
            // Draw grid paper lines overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (showPaperGrid) {
                    val gridSpacing = 40f
                    val dotPaintColor = Color.LightGray.copy(alpha = 0.5f)
                    
                    // Vertical grid dots
                    for (x in 0..(size.width / gridSpacing).toInt()) {
                        for (y in 0..(size.height / gridSpacing).toInt()) {
                            drawCircle(
                                color = dotPaintColor,
                                radius = 2f,
                                center = Offset(x * gridSpacing, y * gridSpacing)
                            )
                        }
                    }
                }

                // Render vector path strokes
                paths.forEach { stroke ->
                    drawPath(
                        path = stroke.path,
                        color = stroke.color,
                        style = Stroke(width = stroke.strokeWidth)
                    )
                }
            }

            if (paths.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                    Text("Illustrate mock diagrams here using fingers", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { paths.clear() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Blank Canvas")
            }

            Button(
                onClick = {
                    val summary = "Scribbled vector blueprint saved containing ${paths.size} active design strokes on a Whiteboard canvas."
                    viewModel.addNote(
                        title = "🎨 Draw: $sketchName",
                        content = summary,
                        tag = "Ideas",
                        colorHex = "Peach",
                        checklistText = ""
                    )
                    onNavigateBack()
                },
                modifier = Modifier.testTag("save_drawing_note_direct")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Publish to Notebook")
            }
        }
    }
}
