package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import com.example.data.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Flashcard(val question: String, val answer: String)
data class QuizQuestion(val question: String, val options: List<String>, val correctIndex: Int)

@Composable
fun StudyAssistantScreen(
    viewModel: MainViewModel,
    notes: List<Note>
) {
    var selectedNoteForStudy by remember { mutableStateOf<Note?>(null) }
    var activeTab by remember { mutableStateOf("Flashcards") } // "Flashcards" or "Quiz Prep"
    val scope = rememberCoroutineScope()
    var isLoadingAid by remember { mutableStateOf(false) }

    // Hardcoded high-yield study templates for standard triggers
    val defaultFlashcards = remember {
        mutableStateListOf(
            Flashcard("What is the Eisenhower Matrix?", "A productivity tool grouping tasks by Urgent vs Important into 4 quadrants."),
            OrginOfNotesFlashcard(),
            Flashcard("What is the golden Rule of Note-taking?", "Summarize into active subtasks and action items immediately."),
            Flashcard("How does the 25-minute Pomodoro rule help?", "It prevents fatigue by incorporating fixed 5-minute break intervals.")
        )
    }

    val defaultQuiz = remember {
        mutableStateListOf(
            QuizQuestion(
                "Which quadrant of the Eisenhower matrix represents delegation?",
                listOf("Urgent & Important", "Urgent & Not Important", "Important & Not Urgent", "Not Urgent & Not Important"),
                1
            ),
            QuizQuestion(
                "What is key for long-term task retention?",
                listOf("Repeated cramming", "Active recall & scheduled flashcards", "Simple text-reading", "Never typing notes"),
                1
            ),
            QuizQuestion(
                "How many minutes is a standard Pomodoro focus cycle?",
                listOf("15 minutes", "45 minutes", "25 minutes", "60 minutes"),
                2
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section Header
        Column {
            Text(
                text = "Academic Study Assistant & Revision Engine",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Use automated active recall flashcards or generated quizzes to check memory pathways.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Active Note Source selector
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🎯 Source Revision Topic from Workspace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (notes.isEmpty()) {
                    Text("No notes found. Create a markdown note to extract study aids!", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(notes) { note ->
                            val isSelected = selectedNoteForStudy?.id == note.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedNoteForStudy = if (isSelected) null else note
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = note.title,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (selectedNoteForStudy != null) {
                        Button(
                            onClick = {
                                isLoadingAid = true
                                scope.launch {
                                    delay(1500)
                                    isLoadingAid = false
                                    // Generate a custom card from the note body dynamically!
                                    val noteText = selectedNoteForStudy?.content ?: ""
                                    val generatedQ = if (noteText.length > 15) noteText.take(40) + "..." else "Review notes of " + selectedNoteForStudy?.title
                                    val generatedA = if (noteText.isNotEmpty()) noteText else "Consult original document references."
                                    defaultFlashcards.add(0, Flashcard(generatedQ, generatedA))
                                    defaultQuiz.add(
                                        0,
                                        QuizQuestion(
                                            "What core topic is discussed in: '${selectedNoteForStudy?.title}'?",
                                            listOf("Personal management logs", "Scientific project outline", "Uncategorized notes data", "Strategic target planning"),
                                            0
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isLoadingAid) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compile Active Recall Material from: '${selectedNoteForStudy?.title}'", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Tab Selector
        TabRow(
            selectedTabIndex = if (activeTab == "Flashcards") 0 else 1,
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = activeTab == "Flashcards",
                onClick = { activeTab = "Flashcards" },
                text = { Text("Active Recall Flashcards") }
            )
            Tab(
                selected = activeTab == "Quiz Prep",
                onClick = { activeTab = "Quiz Prep" },
                text = { Text("Interactive Mock Quiz") }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (activeTab == "Flashcards") {
            FlashcardViewerWidget(defaultFlashcards)
        } else {
            QuizEngineWidget(defaultQuiz)
        }
    }
}

fun OrginOfNotesFlashcard(): Flashcard {
    return Flashcard("How should tag groupings be utilized?", "Tags are used to bundle core tasks and notes across screens seamlessly.")
}

@Composable
fun FlashcardViewerWidget(flashcards: List<Flashcard>) {
    var currentIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    if (flashcards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No flashcards loaded", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val activeCard = flashcards[currentIndex.coerceIn(0, flashcards.size - 1)]

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Counter Label
        Text(
            text = "Flashcard ${currentIndex + 1} of ${flashcards.size}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )

        // Visual Card block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { showAnswer = !showAnswer }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (showAnswer) "ANSWER:" else "ACTIVE RECALL QUESTION:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (showAnswer) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (showAnswer) activeCard.answer else activeCard.question,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (showAnswer) "↩️ Tap back to view question" else "👉 Tap anywhere to reveal answer",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Action controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    showAnswer = false
                    currentIndex = if (currentIndex > 0) currentIndex - 1 else flashcards.size - 1
                }
            ) {
                Text("Previous Card")
            }

            Button(
                onClick = {
                    showAnswer = false
                    currentIndex = (currentIndex + 1) % flashcards.size
                }
            ) {
                Text("Next Flashcard")
            }
        }
    }
}

@Composable
fun QuizEngineWidget(quiz: List<QuizQuestion>) {
    var quizIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var answeredCorrectly by remember { mutableStateOf<Boolean?>(null) }
    var correctAnswersCount by remember { mutableStateOf(0) }

    if (quiz.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Quiz cards configured", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val question = quiz[quizIndex.coerceIn(0, quiz.size - 1)]

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quiz Challenge ${quizIndex + 1} of ${quiz.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )

            Text(
                text = "Score: $correctAnswersCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = question.question,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        question.options.forEachIndexed { optIndex, optionText ->
            val isSelected = selectedOption == optIndex
            val optionBg = when {
                selectedOption != null && optIndex == question.correctIndex -> Color(0xFFD1FAE5) // Success soft Highlight
                isSelected && optIndex != question.correctIndex -> Color(0xFFFEE2E2) // Wrong answer glow
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
            val optionTextCol = when {
                selectedOption != null && optIndex == question.correctIndex -> Color(0xFF065F46)
                isSelected && optIndex != question.correctIndex -> Color(0xFF991B1B)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(optionBg)
                    .clickable(enabled = selectedOption == null) {
                        selectedOption = optIndex
                        val isCorrect = optIndex == question.correctIndex
                        answeredCorrectly = isCorrect
                        if (isCorrect) correctAnswersCount++
                    }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(optionText, fontSize = 13.sp, color = optionTextCol, fontWeight = FontWeight.Medium)
                    if (selectedOption != null && optIndex == question.correctIndex) {
                        Icon(Icons.Default.Check, contentDescription = "Right", tint = Color(0xFF047857))
                    }
                }
            }
        }

        if (selectedOption != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (answeredCorrectly == true) "🎉 Stellar recall! Correct." else "❌ Incorrect. See corrective highlight.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (answeredCorrectly == true) Color(0xFF047857) else Color(0xFFB91C1C)
                )

                Button(
                    onClick = {
                        selectedOption = null
                        answeredCorrectly = null
                        quizIndex = (quizIndex + 1) % quiz.size
                    }
                ) {
                    Text(if (quizIndex + 1 == quiz.size) "Loop Quiz" else "Next Question")
                }
            }
        }
    }
}
