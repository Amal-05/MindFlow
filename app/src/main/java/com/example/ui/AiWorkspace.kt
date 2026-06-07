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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.GeminiClient
import com.example.data.Note
import com.example.data.Task
import kotlinx.coroutines.launch

data class DiscussionBubble(
    val author: String, // "user" or "gemini"
    val content: String,
    val dateText: String
)

@Composable
fun AiWorkspace(
    viewModel: MainViewModel,
    tasks: List<Task>,
    notes: List<Note>,
    onVoiceMemo: () -> Unit = {},
    onOcrScan: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var promptQuery by remember { mutableStateOf("") }
    var isThinkingByAi by remember { mutableStateOf(false) }
    var helperSystemLog by remember { mutableStateOf("Co-pilot ready") }

    val chatDialogue = remember {
        mutableStateListOf(
            DiscussionBubble("gemini", "Greetings! I am your futuristic Mind Flow Intelligence unit. How shall I reorganize, synthesize, or augment your workspace productivity parameters today?", "02:30 PM")
        )
    }

    // Breathing pulse effect properties for premium futuristic look
    val infiniteTransition = rememberInfiniteTransition()
    val pulseGlowVal by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Function to run query against Gemini Model
    val runBrainwaveQuery: (String) -> Unit = { userText ->
        if (userText.isNotBlank()) {
            chatDialogue.add(DiscussionBubble("user", userText, "Just Now"))
            isThinkingByAi = true
            helperSystemLog = "Synchronizing mind nodes..."
            promptQuery = ""
            
            scope.launch {
                try {
                    val rawAnswer = GeminiClient.generateRawText(userText)
                    if (!rawAnswer.isNullOrEmpty()) {
                        chatDialogue.add(DiscussionBubble("gemini", rawAnswer, "Just Now"))
                        helperSystemLog = "Awaiting instructions..."
                    } else {
                        chatDialogue.add(DiscussionBubble("gemini", "I'm having trouble connecting to my cognitive networks. Please verify your GEMINI_API_KEY in the secure Secrets Panel. Here is some offline context.", "Just Now"))
                        helperSystemLog = "Gemini interface returned null."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    chatDialogue.add(DiscussionBubble("gemini", "Apologies, a cognitive load error emerged: ${e.localizedMessage}", "Just Now"))
                    helperSystemLog = "API Connection aborted."
                } finally {
                    isThinkingByAi = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Futuristic Glowing Card Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(1.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Orbital Pulsing AI Orb Icon
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f * pulseGlowVal))
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star, 
                            contentDescription = null, 
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(18.dp)
                        )
                    }
                }

                Column {
                    Text("COGNITIVE INTELLIGENCE CORE", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Synapse connection rating: Optimal. Ask anything or choose quick directives.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Sensory cognitive tools row inside AI screen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onVoiceMemo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("🎙️ Voice Assistant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onOcrScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("📷 OCR Doc Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Suggested Pills Grid Section
        Text(
            text = "⚡ PRE-ENGINEERED DIRECTIVES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Create Tasks" to "Formulate 3 high priority todo directives for organizing an agile backlog sprint",
                "Summarize Notes" to "Analyze and summarize my ongoing workspace notes data: ${notes.take(4).joinToString("; ") { it.title + ": " + it.content }}",
                "Generate Flashcards" to "Design study active recall questions for the notes dataset context: ${notes.take(3).joinToString("; ") { it.content }}",
                "Plan My Day" to "Synthesize a detailed schedule sequence for compiling homework, reviewing tasks, and working out."
            ).forEach { item ->
                ElevatedFilterChip(
                    selected = false,
                    onClick = { runBrainwaveQuery(item.second) },
                    label = { Text(item.first, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Chat Conversation dialogue LazyColumn
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(
                    1.dp, 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), 
                    RoundedCornerShape(20.dp)
                )
                .padding(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(chatDialogue) { bubble ->
                    val isGemini = bubble.author == "gemini"
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isGemini) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalAlignment = if (isGemini) Alignment.Start else Alignment.End
                        ) {
                            Text(
                                text = if (isGemini) "✦ Cognitive Synapse" else "You",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGemini) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isGemini) 4.dp else 16.dp,
                                            bottomEnd = if (isGemini) 16.dp else 4.dp
                                        )
                                    )
                                    .background(
                                        if (isGemini) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = bubble.content,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (isThinkingByAi) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Assembling response matrices...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Action Input keyboard bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptQuery,
                onValueChange = { promptQuery = it },
                placeholder = { Text("Command Gemini Assistant...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_console_input_box"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (promptQuery.isNotBlank() && !isThinkingByAi) {
                        IconButton(onClick = { runBrainwaveQuery(promptQuery) }) {
                            Icon(Icons.Default.Send, contentDescription = "Send text", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    }
}
