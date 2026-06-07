package com.example.ui

import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.Habit
import com.example.data.Task
import kotlinx.coroutines.delay

@Composable
fun DailyHubWorkspace(
    viewModel: MainViewModel,
    habits: List<Habit>,
    tasks: List<Task>,
    onAddHabit: () -> Unit
) {
    var coreTab by remember { mutableStateOf(0) } // 0 = Habits list tracker, 1 = Pomodoro Mode focus sessions

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = coreTab,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ) {
            Tab(selected = coreTab == 0, onClick = { coreTab = 0 }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, "Habits", Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Daily Habits", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = coreTab == 1, onClick = { coreTab = 1 }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, "Pomodoro Mode", Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Focus Session", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (coreTab) {
                0 -> {
                    // Habits tracking panel
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Streaks & Mind Habits", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Button(onClick = onAddHabit) {
                                Icon(Icons.Default.Add, contentDescription = "Add Habit")
                                Spacer(Modifier.width(6.dp))
                                Text("New Habit")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (habits.isEmpty()) {
                            EmptyPlaceholder(
                                icon = Icons.Default.Refresh,
                                title = "Your Streak Tracker is empty",
                                subtitle = "Establish daily exercise milestones or water reminders to compound small activities."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(habits) { habit ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = habit.isCompletedToday,
                                            onCheckedChange = { viewModel.toggleHabit(habit) }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Category: ${habit.category} • Alarm: ${habit.triggerTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }

                                        // Streak Display Counter Badge
                                        Row(
                                            modifier = Modifier
                                                .background(Color(0xFFFFECE0), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, "streak count", tint = Color(0xFFE05B00), modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("${habit.streak}d Streak", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color(0xFFE05B00))
                                        }
                                        
                                        IconButton(onClick = { viewModel.deleteHabit(habit) }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Delete Habit")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> PomodoroTimerWorkspace(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PomodoroTimerWorkspace(viewModel: MainViewModel) {
    val workLength by viewModel.pomodoroWorkIntervalMinutes.collectAsStateWithLifecycle()
    val isFocusActive by viewModel.focusSessionActive.collectAsStateWithLifecycle()
    val secondsCompleted by viewModel.focusDurationCompletedSeconds.collectAsStateWithLifecycle()

    val totalDurationSeconds = workLength * 60
    val secondsRemaining = maxOf(0, totalDurationSeconds - secondsCompleted)
    val minutesLeft = secondsRemaining / 60
    val secsLeft = secondsRemaining % 60
    val timeLabel = String.format("%02d:%02d", minutesLeft, secsLeft)
    
    val percentageProgress = if (totalDurationSeconds == 0) 1f else secondsCompleted.toFloat() / totalDurationSeconds.toFloat()

    // Timer Ticker controller block
    LaunchedEffect(isFocusActive) {
        if (isFocusActive) {
            while (viewModel.focusSessionActive.value) {
                delay(1000)
                viewModel.focusDurationCompletedSeconds.value = viewModel.focusDurationCompletedSeconds.value + 1
                if (viewModel.focusDurationCompletedSeconds.value >= totalDurationSeconds) {
                    viewModel.focusSessionActive.value = false
                    viewModel.focusDurationCompletedSeconds.value = 0
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎯 POMODORO FOCUS COMPLIANCE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Text("Block notifications & align productive cycles.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        Spacer(Modifier.height(32.dp))

        // Visual Pie Gauge Indicator
        Box(
            modifier = Modifier
                .size(220.dp)
                .drawBehind {
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        style = Stroke(width = 16.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFFE05B00),
                        startAngle = -90f,
                        sweepAngle = percentageProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeLabel,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isFocusActive) "Focus Ongoing" else "Idle Zone",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    viewModel.focusSessionActive.value = !isFocusActive
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFocusActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isFocusActive) Icons.Default.Clear else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isFocusActive) "Pause Block" else "Activate Session")
            }

            OutlinedButton(
                onClick = {
                    viewModel.focusSessionActive.value = false
                    viewModel.focusDurationCompletedSeconds.value = 0
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
                Spacer(Modifier.width(6.dp))
                Text("Reset Focus")
            }
        }

        Spacer(Modifier.height(30.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Research proves splitting workloads into 25-minute sprints separated by 5-minute pauses compounds neural recall and prevents fatigue.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
