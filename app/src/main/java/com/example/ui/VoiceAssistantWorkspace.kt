package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TranscribedVoiceNote(val text: String, val timestamp: String, val inferredAction: String)

@Composable
fun VoiceAssistantWorkspace(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var detectedTranscription by remember { mutableStateOf("") }
    var inProgressAction by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    val history = remember {
        mutableStateListOf(
            TranscribedVoiceNote("Buy almond milk and grocery ingredients at 7:00 PM today.", "Tuesday, 4:21 PM", "Created Task: Grocery List"),
            TranscribedVoiceNote("Finish designing the architecture diagram before Friday's standup.", "Yesterday, 10:12 AM", "Created Task: Friday's Standup")
        )
    }

    // Microphone wave animation cycle
    val infiniteTransition = rememberInfiniteTransition()
    val waveHeightMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
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
            Text("AI Vocal Transcriber & Speech Hub", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("Simulate real-time ambient recording, speech-to-text translation and task analysis.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }

        // Recording Wave Controller Area
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRecording) {
                    Text("🔴 Capturing Audio Waveform Signals...", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    // Simple animated voice wave representation
                    Row(
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(0.6f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0.4f, 0.8f, 0.5f, 0.9f, 0.3f, 0.7f, 0.9f, 0.4f, 0.6f).forEach { fraction ->
                            val heightFraction = fraction * waveHeightMultiplier
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                        )
                                    )
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Recorder ready icon", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Vocal Input Channel Idle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (!isRecording) {
                            isRecording = true
                            detectedTranscription = "Hold on, translating speech..."
                            inProgressAction = ""
                            
                            scope.launch {
                                delay(2200) // simulating voice stream
                                val templates = listOf(
                                    "Review the marketing spreadsheets with team at 9:00 AM personal ideas." to "Created Task: Spreadsheet review",
                                    "Brainstorm creative features for mobile app project checklist notes." to "Created Note: Mobile app brainstorming"
                                )
                                val selected = templates.random()
                                detectedTranscription = selected.first
                                inProgressAction = selected.second
                                isRecording = false
                            }
                        } else {
                            isRecording = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("dictate_trigger_action")
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRecording) "Stop Dictation" else "Tap to dictating Voice Commands")
                }
            }
        }

        // Live transcription output
        if (detectedTranscription.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Translated Speech Output:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text(detectedTranscription, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    
                    if (inProgressAction.isNotEmpty() && !isRecording) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨ Core Action Detected: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                            Text(inProgressAction, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Button(
                            onClick = {
                                if (inProgressAction.statusBarActionIsTask()) {
                                    viewModel.addTask(
                                        title = inProgressAction.replace("Created Task: ", ""),
                                        description = detectedTranscription,
                                        priority = "High",
                                        tag = "Personal",
                                        category = "Work",
                                        dueDateLong = System.currentTimeMillis() + 86400000L,
                                        dueTime = "06:00 PM"
                                    )
                                } else {
                                    viewModel.addNote(
                                        title = inProgressAction.replace("Created Note: ", ""),
                                        content = detectedTranscription,
                                        tag = "Ideas",
                                        colorHex = "Blue",
                                        checklistText = ""
                                    )
                                }
                                history.add(0, TranscribedVoiceNote(detectedTranscription, "Just Now", inProgressAction))
                                detectedTranscription = ""
                                inProgressAction = ""
                            },
                            modifier = Modifier.fillMaxWidth().testTag("apply_voice_command_output")
                        ) {
                            Text("Confirm & Inject Workspace Entry")
                        }
                    }
                }
            }
        }

        // History logs
        Text("📖 Active Vocal Activity Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Vocal record", tint = MaterialTheme.colorScheme.outline)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.text, fontSize = 12.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(record.timestamp, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            Text("🕒 ACTION: ${record.inferredAction}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun String.statusBarActionIsTask(): Boolean {
    return this.contains("Task")
}
