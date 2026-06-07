package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.Task
import com.example.data.Note
import com.example.data.GeminiClient
import kotlinx.coroutines.launch

data class MoodLog(val emotion: String, val text: String, val timestamp: String)

@Composable
fun LifeDashboardScreen(
    viewModel: MainViewModel,
    tasks: List<Task>,
    notes: List<Note>
) {
    var selectedMood by remember { mutableStateOf("⚡ Productive") }
    var moodDiaryText by remember { mutableStateOf("") }
    val moodLogs = remember {
        mutableStateListOf(
            MoodLog("🚀 Energetic", "Smashed my 3 morning Pomodoros! Feeling great about work.", "09:12 AM"),
            MoodLog("🧘 Focused", "Finished reading the strategy matrix and compiled study guide.", "Yesterday")
        )
    }

    var selectedMindMapNode by remember { mutableStateOf("Main Goals") }
    var mindMapGoalText_1 by remember { mutableStateOf("Read 2 books monthly") }
    var mindMapGoalText_2 by remember { mutableStateOf("Exercise 3x a week") }
    var mindMapGoalText_3 by remember { mutableStateOf("Execute 5 Daily Habits") }

    var userCustomChatMessage by remember { mutableStateOf("") }
    val aiChatConversation = remember {
        mutableStateListOf(
            "Insight Executive Coach" to "Welcome back! What are we focusing on today? Write below to plan."
        )
    }
    val scope = rememberCoroutineScope()
    var isThinking by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section Header
        item {
            Column {
                Text(
                    text = "Life Dashboard & Vision board",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Combine core productivity statistics with conscious emotional logging & goals mapping.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Integration bar
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Google Calendar Synchronizer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Automatic secure cloud replication active.", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Active", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mood Diary Tracker Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Daily Mood Logging & Gratitude", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    val moods = listOf("⚡ Productive", "🧘 Focused", "🚀 Energetic", "😴 Tired", "😇 Relaxed")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(moods) { mood ->
                            val isSelected = selectedMood == mood
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedMood = mood }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mood,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = moodDiaryText,
                        onValueChange = { moodDiaryText = it },
                        placeholder = { Text("What are you grateful for right now?") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (moodDiaryText.isNotEmpty()) {
                                moodLogs.add(0, MoodLog(selectedMood, moodDiaryText, "10:57 AM"))
                                moodDiaryText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Post Mood Entry")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Previous Mood Log listing
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        moodLogs.take(3).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(log.emotion, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(log.text, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Text(log.timestamp, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        // Mind Mapping Board Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Goal Mapping Flowchart",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    // Core Mind Map Node
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Ultimate Dream Plan",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }

                    // Radiating Node edits
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mindMapGoalText_1,
                            onValueChange = { mindMapGoalText_1 = it },
                            label = { Text("Education Core Node") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = mindMapGoalText_2,
                            onValueChange = { mindMapGoalText_2 = it },
                            label = { Text("Fitness Core Node") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = mindMapGoalText_3,
                            onValueChange = { mindMapGoalText_3 = it },
                            label = { Text("Habits & Rituals Node") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Dedicated AI Coaching Assistant
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "🔮 Generative Personal AI Coach",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text("Chat directly with Gemini to resolve complex goals challenges or get schedule reviews.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                    // Chat conversation column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(aiChatConversation) { message ->
                                Column {
                                    Text(
                                        message.first,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (message.first == "You") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(message.second, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = userCustomChatMessage,
                        onValueChange = { userCustomChatMessage = it },
                        placeholder = { Text("Ask Coach Gemini anything...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isThinking) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                IconButton(
                                    onClick = {
                                        val prompt = userCustomChatMessage
                                        if (prompt.isNotEmpty()) {
                                            aiChatConversation.add("You" to prompt)
                                            userCustomChatMessage = ""
                                            isThinking = true
                                            scope.launch {
                                                try {
                                                    val aiReply = runGeminiCoachPrompt(prompt, tasks, notes)
                                                    aiChatConversation.add("Gemini" to aiReply)
                                                } catch (e: Exception) {
                                                    aiChatConversation.add("Gemini" to "AI is offline. Make sure Gemini API Key is configured. Error: ${e.localizedMessage}")
                                                } finally {
                                                    isThinking = false
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send message")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

suspend fun runGeminiCoachPrompt(userPrompt: String, tasks: List<Task>, notes: List<Note>): String {
    val promptFull = """
        You are an advanced executive life coach helping a user optimize their tasks and journals.
        Tasks list status currently:
        ${tasks.take(4).joinToString("\n") { "- ${it.title} (${it.status})" }}
        
        Notes list:
        ${notes.take(4).joinToString("\n") { "- ${it.title}" }}
        
        Question: $userPrompt
        Answer in 1-2 powerful, encouraging sentences.
    """.trimIndent()
    
    return GeminiClient.generateRawText(promptFull) ?: "I am confident we can tackle these action items. Focus on urgent priorities first!"
}
