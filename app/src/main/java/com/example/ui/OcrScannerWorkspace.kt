package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OcrScannerWorkspace(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedDocType by remember { mutableStateOf("Invoice Receipt") }
    var scaleScanning by remember { mutableStateOf(false) }
    var extractedTextResult by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

    // Neon green scanning bar animation properties
    val infiniteTransition = rememberInfiniteTransition()
    val scanOffsetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("OCR Document Digitizer & Scanner", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("Simulate live viewfinder camera alignments and convert papers to editable text.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Invoice Receipt", "Meeting Board", "Class Note").forEach { doc ->
                val active = doc == selectedDocType
                ElevatedFilterChip(
                    selected = active,
                    onClick = { selectedDocType = doc },
                    label = { Text(doc) }
                )
            }
        }

        // Viewfinder block simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Simulated camera perspective guidelines
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (scaleScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(130.dp)
                            .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                    ) {
                        // Sliding beam scanning line representation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = (scanOffsetProgress * 130f).dp)
                                .background(Color(0xFF10B981))
                        )

                        Text(
                            text = "[ ALIGNING CHIPS... ]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(100.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Frame receipt/notes outline within this marker.",
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Small watermark overlay
            Text(
                "CAMERA SENSOR FEED [SIMULA]",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }

        // Bottom triggers action and extract results
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    scaleScanning = true
                    scope.launch {
                        delay(2000)
                        extractedTextResult = when (selectedDocType) {
                            "Invoice Receipt" -> "BILL AT STARBUCKS #305\n- Mocha Latte ($5.40)\n- Almond Croissant ($4.50)\nTotal spent: $9.90. Sync invoice."
                            "Meeting Board" -> "MOCK BOARD STRATEGY:\n- Fix dependency version issues by Wed\n- Review product catalog mockup sheets\n- Complete Robolectric tests coverage."
                            else -> "CLASS SLATE RECONSTRUCTIONS:\nNeural nets parameters: depth = 12, alpha factor = 0.05. Read documentation."
                        }
                        scaleScanning = false
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !scaleScanning,
                modifier = Modifier.testTag("activate_ocr_sensor_action")
            ) {
                Icon(Icons.Default.Menu, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (scaleScanning) "Analyzing Text Matrix..." else "Scan & Transcribe Document")
            }
        }

        // Expanded text results and import choices
        if (extractedTextResult.isNotEmpty() && !scaleScanning) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Scanned OCR Text Captured", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    
                    Text(
                        text = extractedTextResult,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.addNote(
                                    title = "📄 OCR: $selectedDocType",
                                    content = extractedTextResult,
                                    tag = "Work",
                                    colorHex = "Blue",
                                    checklistText = ""
                                )
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_ocr_as_note")
                        ) {
                            Text("Import Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.addTask(
                                    title = "OCR Task: Confirm $selectedDocType",
                                    description = extractedTextResult,
                                    priority = "Medium",
                                    tag = "Work",
                                    category = "Work",
                                    dueDateLong = System.currentTimeMillis() + 86400000L,
                                    dueTime = "06:00 PM"
                                )
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_ocr_as_task")
                        ) {
                            Text("Create Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
