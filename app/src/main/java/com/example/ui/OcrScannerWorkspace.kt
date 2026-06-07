package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.GeminiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScannerWorkspace(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDocType by remember { mutableStateOf("Invoice Receipt") }
    var activeTabOfOcr by remember { mutableStateOf(0) } // 0 = Viewfinder Scan, 1 = Upload Gallery, 2 = Manual Sandbox
    
    var scaleScanning by remember { mutableStateOf(false) }
    var rawTextPasteInput by remember { mutableStateOf("") }
    var extractedTextResult by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var systemLogMessage by remember { mutableStateOf("") }

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

    // Image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            systemLogMessage = "Selected image successfully. Tap below to run Cloud AI scan."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("OCR Document Digitizer & Scanner", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text("Instantly convert paper notebooks, invoices, and physical slates into editable tasks or notes.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }

        // Feature Selector Tabs
        TabRow(
            selectedTabIndex = activeTabOfOcr,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTabOfOcr == 0,
                onClick = { activeTabOfOcr = 0; extractedTextResult = "" },
                text = { Text("Viewfinder", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeTabOfOcr == 1,
                onClick = { activeTabOfOcr = 1; extractedTextResult = "" },
                text = { Text("Upload Image", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeTabOfOcr == 2,
                onClick = { activeTabOfOcr = 2; extractedTextResult = "" },
                text = { Text("Text Sandbox", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        if (activeTabOfOcr == 0) {
            // VIEW_FINDER PRESETS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Invoice Receipt", "Meeting Board", "Class Note").forEach { doc ->
                    val active = doc == selectedDocType
                    ElevatedFilterChip(
                        selected = active,
                        onClick = { selectedDocType = doc },
                        label = { Text(doc, fontSize = 11.sp) }
                    )
                }
            }

            // Viewfinder block simulation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.DarkGray)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (scaleScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(130.dp)
                            .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = (scanOffsetProgress * 130f).dp)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "[ ALIGNING CHIPS & SCANNING TEXT... ]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(100.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Standard Viewfinder Mode.\nHold receipt or class notebook inside card.",
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Text(
                    "DIGITAL CAMERA PREVIEW [SIMULATION]",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        scaleScanning = true
                        scope.launch {
                            delay(1800)
                            extractedTextResult = when (selectedDocType) {
                                "Invoice Receipt" -> "BILL AT STARBUCKS #305\n- Mocha Latte ($5.40)\n- Almond Croissant ($4.50)\nTotal spent: $9.90. Sync invoice."
                                "Meeting Board" -> "MOCK BOARD STRATEGY:\n- Fix dependency version issues by Wed\n- Review product catalog mockup sheets\n- Complete Robolectric tests coverage."
                                else -> "CLASS SLATE RECONSTRUCTIONS:\nNeural nets parameters: depth = 12, alpha factor = 0.05. Read documentation."
                            }
                            scaleScanning = false
                            systemLogMessage = "Simulated document analyzed successfully."
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !scaleScanning,
                    modifier = Modifier.fillMaxWidth().testTag("activate_ocr_sensor_action")
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (scaleScanning) "Analyzing Matrix..." else "Simulate Viewfinder Live Scan")
                }
            }
        } else if (activeTabOfOcr == 1) {
            // UPLOAD IMAGE FROM GALLERY (REAL METRICS)
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Select Real Image Document for Cloud AI OCR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("We'll send the file to Gemini 1.5 Flash to automatically interpret any handwritings, notes or text directly from your gallery images.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)

                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedImageUri != null) "Choose Different Image" else "Browse Gallery")
                    }

                    if (selectedImageUri != null) {
                        Text(
                            text = "Selected Path: ${selectedImageUri.toString()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )

                        Button(
                            onClick = {
                                scaleScanning = true
                                systemLogMessage = "Extracting bytes & calling Gemini API..."
                                scope.launch {
                                    val base64 = selectedImageUri?.toBase64(context)
                                    if (base64 == null) {
                                        systemLogMessage = "Failed to convert selected image to base64 bytes."
                                        scaleScanning = false
                                        return@launch
                                    }
                                    val result = GeminiClient.analyzeImageOcr(base64, "image/jpeg")
                                    if (result != null) {
                                        extractedTextResult = result
                                        systemLogMessage = "Real Gemini OCR completed successfully!"
                                    } else {
                                        systemLogMessage = "Cloud OCR failed or API returned empty. Ensure a valid GEMINI_API_KEY is configured."
                                    }
                                    scaleScanning = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            enabled = !scaleScanning,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (scaleScanning) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Analyzing with Cloud Gemini...")
                            } else {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Perform Real Gemini OCR Scan")
                            }
                        }
                    }
                }
            }
        } else {
            // MANUAL SANDBOX TEXT ACCELERATION
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Keyboard Sandbox Parser", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Type or paste any text below to test how the digitizer system translates custom content instantly into workspace entities.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                    OutlinedTextField(
                        value = rawTextPasteInput,
                        onValueChange = { rawTextPasteInput = it },
                        placeholder = { Text("Paste note text, invoice details, receipts or brainstorm dumps here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Button(
                        onClick = {
                            if (rawTextPasteInput.isNotBlank()) {
                                extractedTextResult = rawTextPasteInput
                                systemLogMessage = "Note sandbox captured successfully."
                            } else {
                                systemLogMessage = "Please write or paste something first."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Capture Text Frame")
                    }
                }
            }
        }

        // Status logger
        if (systemLogMessage.isNotEmpty()) {
            Text(
                text = "SYSTEM LOG: $systemLogMessage",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Expanded text results and import choices
        if (extractedTextResult.isNotEmpty() && !scaleScanning) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Extracted Document Content Recognized", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    title = "📄 OCR Scan: ${selectedDocType}",
                                    content = extractedTextResult,
                                    tag = "Work",
                                    colorHex = "#38BDF8",
                                    checklistText = ""
                                )
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_ocr_as_note")
                        ) {
                            Text("Create Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.addTask(
                                    title = "Digitized Reference: " + extractedTextResult.lineSequence().firstOrNull()?.take(30).orEmpty(),
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

// Extension function to help Uri conversion to base64 encoding safely
fun Uri.toBase64(context: android.content.Context): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(this)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
