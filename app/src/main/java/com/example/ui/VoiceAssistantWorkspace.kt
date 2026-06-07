package com.example.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.MainViewModel
import com.example.data.GeminiClient
import com.example.data.Note
import com.example.data.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ActiveVoiceCommandLog(
    val spokenText: String,
    val dateText: String,
    val resolvedIntent: String,
    val wasSuccessful: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantWorkspace(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var detectedTranscription by remember { mutableStateOf("") }
    var systemLogMessage by remember { mutableStateOf("Ready to receive voice command.") }
    var isAnalyzingWithGemini by remember { mutableStateOf(false) }

    // Manual typing feedback parameters
    var manualTypedCommand by remember { mutableStateOf("") }

    // Extracted intelligence results
    var parsedActionType by remember { mutableStateOf("") } // CREATE_TASK, CREATE_NOTE, SEARCH_ITEMS, UNKNOWN
    var interpretedTitle by remember { mutableStateOf("") }
    var interpretedDetails by remember { mutableStateOf("") }
    var interpretedPriority by remember { mutableStateOf("Medium") }
    var interpretedTag by remember { mutableStateOf("Personal") }

    // Local state log history
    val commandLogs = remember {
        mutableStateListOf(
            ActiveVoiceCommandLog("remind me to complete weekly metrics reporting tonight high priority", "Today, 10:45 AM", "RESOLVED: Created Task - Weekly Metrics", true),
            ActiveVoiceCommandLog("take note of a secret design pattern for neural routing layers", "Yesterday, 02:15 PM", "RESOLVED: Created Note - Design Pattern Ideas", true)
        )
    }

    // Interactive speech wave visualization properties
    val infiniteTransition = rememberInfiniteTransition()
    val waveScaler by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // SpeechRecognizer initialization & callbacks
    val speechModelIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    val speechRecognizer = remember {
        try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            null
        }
    }

    // Function to process transcribed voice using Gemini Smart Parsing
    val runSmartNlpParser: (String) -> Unit = { commandText ->
        if (commandText.isNotBlank()) {
            isAnalyzingWithGemini = true
            systemLogMessage = "AI Agent analyzing natural language intent..."
            scope.launch {
                try {
                    val rawJson = GeminiClient.interpretVoiceCommand(commandText)
                    if (!rawJson.isNullOrEmpty()) {
                        // Clean markdown wrapping
                        var cleanJson = rawJson.trim()
                        if (cleanJson.startsWith("```json")) {
                            cleanJson = cleanJson.substring(7)
                        }
                        if (cleanJson.endsWith("```")) {
                            cleanJson = cleanJson.substring(0, cleanJson.length - 3)
                        }
                        cleanJson = cleanJson.trim()

                        val parsedObj = JSONObject(cleanJson)
                        parsedActionType = parsedObj.optString("action", "UNKNOWN")
                        interpretedTitle = parsedObj.optString("title", "Voice Entry")
                        interpretedDetails = parsedObj.optString("details", commandText)
                        interpretedPriority = parsedObj.optString("priority", "Medium")
                        interpretedTag = parsedObj.optString("tag", "Personal")

                        if (parsedActionType == "SEARCH_ITEMS") {
                            viewModel.setSearchQuery(interpretedDetails)
                            systemLogMessage = "Search query set automatically!"
                        } else {
                            systemLogMessage = "Intent Extracted: $parsedActionType!"
                        }
                    } else {
                        // Fallback manual heuristic parse if API fails
                        if (commandText.contains("todo", ignoreCase = true) || commandText.contains("remind", ignoreCase = true) || commandText.contains("task", ignoreCase = true)) {
                            parsedActionType = "CREATE_TASK"
                            interpretedTitle = "Vocal Directive: " + commandText.take(25)
                            interpretedDetails = commandText
                        } else {
                            parsedActionType = "CREATE_NOTE"
                            interpretedTitle = "Vocal Dictation memo"
                            interpretedDetails = commandText
                        }
                        systemLogMessage = "Heuristic parsing fallback active."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    systemLogMessage = "Interpretation error: " + e.localizedMessage
                } finally {
                    isAnalyzingWithGemini = false
                }
            }
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                systemLogMessage = "Speak now..."
            }
            override fun onBeginningOfSpeech() {
                systemLogMessage = "Recording active..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                systemLogMessage = "Capturing finish, analyzing..."
            }
            override fun onError(error: Int) {
                isRecording = false
                systemLogMessage = "System Voice SpeechRecognizer index $error offline."
            }
            override fun onResults(results: Bundle?) {
                isRecording = false
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!list.isNullOrEmpty()) {
                    val speechResult = list[0]
                    detectedTranscription = speechResult
                    runSmartNlpParser(speechResult)
                } else {
                    systemLogMessage = "No audibles deciphered."
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!list.isNullOrEmpty()) {
                    detectedTranscription = list[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    // Mic recording permission launcher
    val launchRecordAudioPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && speechRecognizer != null) {
            isRecording = true
            detectedTranscription = ""
            systemLogMessage = "Initializing microphone input..."
            try {
                speechRecognizer.startListening(speechModelIntent)
            } catch (e: Exception) {
                systemLogMessage = "Mic input error: ${e.localizedMessage}"
                isRecording = false
            }
        } else {
            systemLogMessage = "Microphone permissions denied. Please type commands below manually."
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
            Text("AI Vocal Transcriber & Speech Hub", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text("Command your workspace via speech. Dictate to create tasks, note secrets, or search entries instantly.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }

        // Animated Micro-phone Wave Recording Box
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
                    Text("🔴 Capturing Ambient Vocal Modulation", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier
                            .height(55.dp)
                            .fillMaxWidth(0.6f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0.4f, 0.8f, 0.5f, 0.9f, 0.3f, 0.7f, 0.9f, 0.4f, 0.6f).forEach { fraction ->
                            val dynamicHeight = fraction * waveScaler
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(dynamicHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                        )
                                    )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isRecording = false
                            speechRecognizer?.stopListening()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hold & Stop Input")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text("Speech-Engine Idle", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Button(
                        onClick = {
                            val status = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                            if (status == PackageManager.PERMISSION_GRANTED) {
                                isRecording = true
                                detectedTranscription = ""
                                systemLogMessage = "Activating microphone sensor..."
                                try {
                                    if (speechRecognizer != null) {
                                        speechRecognizer.startListening(speechModelIntent)
                                    } else {
                                        // Simulator fallback when SpeechRecognizer framework is not fully supported
                                        scope.launch {
                                            delay(1500)
                                            detectedTranscription = listOf(
                                                "remind me to check homework project specs tomorrow at 9 AM",
                                                "take note that creative focus session needs warm workspace colors",
                                                "search workspace for ml layers notes"
                                            ).random()
                                            isRecording = false
                                            runSmartNlpParser(detectedTranscription)
                                        }
                                    }
                                } catch (e: Exception) {
                                    systemLogMessage = "Audio system start error: " + e.localizedMessage
                                    isRecording = false
                                }
                            } else {
                                launchRecordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dictate_trigger_action")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Simulate or Record Speech Command")
                    }
                }
            }
        }

        // MANUAL VOCAL COMMAND KEYPAD CONSOLE
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Type Speech Console (No Microphone Required)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Type your vocal phrase directly. Our smart NLP engine analyzes it in the same exact way.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                OutlinedTextField(
                    value = manualTypedCommand,
                    onValueChange = { manualTypedCommand = it },
                    placeholder = { Text("Example: 'remind me to run 5km tomorrow high priority' or 'search for study cards'...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (manualTypedCommand.isNotBlank()) {
                                    detectedTranscription = manualTypedCommand
                                    runSmartNlpParser(manualTypedCommand)
                                    manualTypedCommand = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }

        // Live system notification feedback
        Text(
            text = "AGENT STATUS: $systemLogMessage",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // LOADING INDICATOR
        if (isAnalyzingWithGemini) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        // DISPLAY INTERPRETED ACTION & DYNAMIC FORM INSERTION
        if (detectedTranscription.isNotEmpty() && !isRecording && !isAnalyzingWithGemini) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Speech Decoded Successfully", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Text(
                        text = "\"$detectedTranscription\"",
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text("AI Brain Interpretation Results:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("ACTION: $parsedActionType") }
                        )
                        if (parsedActionType != "SEARCH_ITEMS" && parsedActionType != "UNKNOWN") {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("TAG: $interpretedTag") }
                            )
                        }
                    }

                    if (parsedActionType == "SEARCH_ITEMS") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Column {
                                    Text("Dynamic Workspace Query Firing", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Searching for: \"$interpretedDetails\"", fontSize = 11.sp)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.setSearchQuery(interpretedDetails)
                                commandLogs.add(0, ActiveVoiceCommandLog(detectedTranscription, "Just Now", "RESOLVED: Triggered Search for '$interpretedDetails'", true))
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Take Me to Search results")
                        }
                    } else if (parsedActionType == "CREATE_TASK") {
                        OutlinedTextField(
                            value = interpretedTitle,
                            onValueChange = { interpretedTitle = it },
                            label = { Text("Identified Task Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = interpretedDetails,
                            onValueChange = { interpretedDetails = it },
                            label = { Text("Task Description Detail") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.addTask(
                                    title = interpretedTitle,
                                    description = interpretedDetails,
                                    priority = interpretedPriority,
                                    tag = interpretedTag,
                                    category = "Work",
                                    dueDateLong = System.currentTimeMillis() + 86400000L,
                                    dueTime = "06:00 PM"
                                )
                                commandLogs.add(0, ActiveVoiceCommandLog(detectedTranscription, "Just Now", "RESOLVED: Created Task - $interpretedTitle", true))
                                detectedTranscription = ""
                                parsedActionType = ""
                                systemLogMessage = "Task inserted directly into Workspace database!"
                            },
                            modifier = Modifier.fillMaxWidth().testTag("apply_voice_command_output")
                        ) {
                            Text("Confirm & Insert Task", fontWeight = FontWeight.Bold)
                        }
                    } else if (parsedActionType == "CREATE_NOTE") {
                        OutlinedTextField(
                            value = interpretedTitle,
                            onValueChange = { interpretedTitle = it },
                            label = { Text("Identified Note Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = interpretedDetails,
                            onValueChange = { interpretedDetails = it },
                            label = { Text("Note Content Detail") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.addNote(
                                    title = interpretedTitle,
                                    content = interpretedDetails,
                                    tag = interpretedTag,
                                    colorHex = "#A78BFA",
                                    checklistText = ""
                                )
                                commandLogs.add(0, ActiveVoiceCommandLog(detectedTranscription, "Just Now", "RESOLVED: Created Note - $interpretedTitle", true))
                                detectedTranscription = ""
                                parsedActionType = ""
                                systemLogMessage = "Note inserted directly into Workspace database!"
                            },
                            modifier = Modifier.fillMaxWidth().testTag("apply_voice_command_output")
                        ) {
                            Text("Confirm & Insert Note", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // FALLBACK BUTTON
                        Button(
                            onClick = {
                                viewModel.addNote(
                                    title = "Spoken Dictation Log",
                                    content = detectedTranscription,
                                    tag = "Personal",
                                    colorHex = "#FF8BFA",
                                    checklistText = ""
                                )
                                commandLogs.add(0, ActiveVoiceCommandLog(detectedTranscription, "Just Now", "RESOLVED: Backup audio logs saved", true))
                                detectedTranscription = ""
                                parsedActionType = ""
                                systemLogMessage = "Fallback Backup note saved."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import as plain Vocal Memo Note")
                        }
                    }
                }
            }
        }

        // DICTATION RECENT LOG HISTORIES
        Text("📖 Active Vocal Activity Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (commandLogs.isEmpty()) {
                    Text("No local voice activities recorded yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(8.dp))
                } else {
                    commandLogs.forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\"${log.spokenText}\"", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.dateText, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(log.resolvedIntent, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
