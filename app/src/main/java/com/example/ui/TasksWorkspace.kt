package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksWorkspace(
    viewModel: MainViewModel,
    tasks: List<Task>,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit
) {
    var filterTab by remember { mutableStateOf(0) } // 0 = Standard list, 1 = Calendar View, 2 = Eisenhower Quadrants
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Mode Select Bar
        TabRow(
            selectedTabIndex = filterTab,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ) {
            Tab(selected = filterTab == 0, onClick = { filterTab = 0 }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, "List", Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Workspace", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = filterTab == 1, onClick = { filterTab = 1 }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, "Calendar", Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Calendar Board", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = filterTab == 2, onClick = { filterTab = 2 }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbUp, "Matrix", Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eisenhower", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal Category Chips
        CategorySelectorsBar(
            selectedTag = selectedTag,
            onTagSelected = { viewModel.setTag(it) }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (filterTab) {
                0 -> {
                    // Task Lists divided by Complete & Incomplete status
                    if (tasks.isEmpty()) {
                        EmptyPlaceholder(
                            icon = Icons.Default.CheckCircle,
                            title = "Task List is clear",
                            subtitle = "Tap add button below to plan out your high importance meetings, milestones or routine items."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("In Progress Schedules", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    Text("${tasks.count { !it.isCompleted }} pending", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            items(tasks.filter { !it.isCompleted }) { task ->
                                ComplexTaskCard(
                                    task = task,
                                    onToggle = { viewModel.toggleTask(task) },
                                    onClick = { onEditTask(task) },
                                    onDelete = { viewModel.deleteTask(task) },
                                    onToggleSubtask = { index ->
                                        val subtasks = task.getSubtasks().toMutableList()
                                        val old = subtasks[index]
                                        subtasks[index] = old.copy(isChecked = !old.isChecked)
                                        val updatedText = Task.serializeSubtasks(subtasks)
                                        viewModel.updateTask(task.copy(subtasksText = updatedText))
                                    }
                                )
                            }

                            val completedTasks = tasks.filter { it.isCompleted }
                            if (completedTasks.isNotEmpty()) {
                                item {
                                    Text("Archived Complete (${completedTasks.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
                                }
                                items(completedTasks) { task ->
                                    ComplexTaskCard(
                                        task = task,
                                        onToggle = { viewModel.toggleTask(task) },
                                        onClick = { onEditTask(task) },
                                        onDelete = { viewModel.deleteTask(task) },
                                        onToggleSubtask = { index ->
                                            val subtasks = task.getSubtasks().toMutableList()
                                            val old = subtasks[index]
                                            subtasks[index] = old.copy(isChecked = !old.isChecked)
                                            val updatedText = Task.serializeSubtasks(subtasks)
                                            viewModel.updateTask(task.copy(subtasksText = updatedText))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> CalendarInteractiveView(tasks = tasks, onSelectTask = onEditTask)
                2 -> EisenhowerMatrixBoard(tasks = tasks, onSelectTask = onEditTask)
            }
        }

        // Bottom Add Floating Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onAddTask,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_task_floating_secondary")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
                Spacer(Modifier.width(6.dp))
                Text("Compose Schedule", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategorySelectorsBar(selectedTag: String, onTagSelected: (String) -> Unit) {
    val items = listOf("All", "Work", "Personal", "Ideas", "Urgent")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { text ->
            val sel = text == selectedTag
            FilterChip(
                selected = sel,
                onClick = { onTagSelected(text) },
                label = { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComplexTaskCard(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleSubtask: (Int) -> Unit
) {
    val progress = task.getProgressPercentage()
    val subtasks = task.getSubtasks()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    if (task.noteText.isNotEmpty()) {
                        Text(
                            text = task.noteText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Drop Task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Metatags, Priority level and Calendar Date info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Badge
                Box(
                    modifier = Modifier
                        .background(
                            when (task.priority) {
                                "Critical" -> Color(0xFFFFDAD9)
                                "High" -> Color(0xFFFFDDB3)
                                "Medium" -> Color(0xFFE0E7FF)
                                else -> Color(0xFFD2E8D4)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.priority.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color.Black
                    )
                }

                // Category & Tag badges
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(Modifier.weight(1f))

                if (task.dueDateLong > 0L) {
                    val formatted = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(task.dueDateLong))
                    Text(
                        text = "⏳ $formatted ${task.dueTime}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Subtask checklists expansion block
            if (subtasks.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Checklist Subtasks ($progress%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(4.dp))
                
                subtasks.forEachIndexed { index, sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSubtask(index) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (sub.isChecked) Icons.Default.CheckCircle else Icons.Default.Star,
                            contentDescription = "subtask",
                            modifier = Modifier.size(14.dp),
                            tint = if (sub.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = sub.text,
                            fontSize = 12.sp,
                            textDecoration = if (sub.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (sub.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarInteractiveView(tasks: List<Task>, onSelectTask: (Task) -> Unit) {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    
    val weekDays = remember {
        val list = mutableListOf<Date>()
        cal.set(Calendar.DAY_OF_MONTH, todayDay - 3)
        (0..7).forEach { _ ->
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }
    
    var selectedDate by remember { mutableStateOf(Date()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Weekly Schedule Timeline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEach { date ->
                val simpleDate = SimpleDateFormat("dd", Locale.getDefault()).format(date)
                val simpleWeekDay = SimpleDateFormat("EEE", Locale.getDefault()).format(date).take(2)
                val isSel = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date) == 
                               SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(selectedDate)
                
                Box(
                    modifier = Modifier
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedDate = date }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = simpleWeekDay,
                            fontSize = 9.sp,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = simpleDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Milestones Due on this date:",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(8.dp))

        val selectedStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(selectedDate)
        val matches = tasks.filter { task ->
            task.dueDateLong > 0L && SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(task.dueDateLong)) == selectedStr
        }

        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Zero schedules or deadlines due today.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matches) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelectTask(task) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Due item", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Time slot: ${task.dueTime} • Category: ${task.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EisenhowerMatrixBoard(tasks: List<Task>, onSelectTask: (Task) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Eisenhower Priority Quadrants", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Sort items clearly based on urgency rules.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quadrant 1: Critical Tasks
            QuadrantContainer(
                title = "🔥 IMPORTANT & URGENT (Do first)",
                bgColor = Color(0xFFFFDAD9),
                titleColor = Color(0xFF410002),
                tasksList = tasks.filter { it.priority == "Critical" && !it.isCompleted },
                onSelect = onSelectTask
            )

            // Quadrant 2: High priority
            QuadrantContainer(
                title = "📅 IMPORTANT & NOT URGENT (Plan next)",
                bgColor = Color(0xFFFFECCC),
                titleColor = Color(0xFF5E2B00),
                tasksList = tasks.filter { it.priority == "High" && !it.isCompleted },
                onSelect = onSelectTask
            )

            // Quadrant 3: Medium priority
            QuadrantContainer(
                title = "🤝 DELEGATE (Urgent but low priority)",
                bgColor = Color(0xFFE0E7FF),
                titleColor = Color(0xFF3730A3),
                tasksList = tasks.filter { it.priority == "Medium" && !it.isCompleted },
                onSelect = onSelectTask
            )

            // Quadrant 4: Low priority
            QuadrantContainer(
                title = "🗑️ ELIMINATE / MOCK IDEAS",
                bgColor = Color(0xFFF1F5F9),
                titleColor = Color(0xFF334155),
                tasksList = tasks.filter { it.priority == "Low" && !it.isCompleted },
                onSelect = onSelectTask
            )
        }
    }
}

@Composable
fun ColumnScope.QuadrantContainer(
    title: String,
    bgColor: Color,
    titleColor: Color,
    tasksList: List<Task>,
    onSelect: (Task) -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(10.dp)
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = titleColor)
            Spacer(Modifier.height(4.dp))
            if (tasksList.isEmpty()) {
                Text("Zero tasks in this sector.", fontSize = 11.sp, color = titleColor.copy(alpha = 0.5f))
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasksList) { task ->
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .clickable { onSelect(task) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(task.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
