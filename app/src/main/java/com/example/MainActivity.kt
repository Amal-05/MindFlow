package com.example

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = DatabaseProvider.getRepository(this)
        val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

class MainViewModelFactory(private val repository: TaskAndNoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Tasks, 1 = Notes
    
    // Dialog control states
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }

    // State flows
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "App Icon Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Notes & Tasks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeTab == 0) {
                        taskToEdit = null
                        showTaskDialog = true
                    } else {
                        noteToEdit = null
                        showNoteDialog = true
                    }
                },
                modifier = Modifier.testTag(if (activeTab == 0) "add_task_fab" else "add_note_fab"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (activeTab == 0) "Add Task" else "Add Note"
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar & Query Builder
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search records, topics, or checklist items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Segmented Workspace Navigator (Tabs)
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "Tasks Icon", Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pending Todos (${tasks.count { !it.isCompleted }})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, "Notes Icon", Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Rich Notes (${notes.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            // Scrollable tag categories at the top level
            CategoryFilterBar(
                selectedTag = selectedTag,
                onTagSelected = { viewModel.setTag(it) }
            )

            // Dynamic Content Renderer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (activeTab == 0) {
                    TasksWorkspace(
                        tasks = tasks,
                        onToggleTask = { viewModel.toggleTask(it) },
                        onEditTask = {
                            taskToEdit = it
                            showTaskDialog = true
                        },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                } else {
                    NotesWorkspace(
                        notes = notes,
                        onEditNote = {
                            noteToEdit = it
                            showNoteDialog = true
                        },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onTriggerAiSummary = { viewModel.summarizeAndGenerateTasksFromNote(it) }
                    )
                }
            }
        }
    }

    // Task Create / Update dialog
    if (showTaskDialog) {
        TaskEditorDialog(
            task = taskToEdit,
            onDismiss = { showTaskDialog = false },
            onSave = { title, desc, p, t, date ->
                if (taskToEdit == null) {
                    viewModel.addTask(title, desc, p, t, date)
                } else {
                    viewModel.updateTask(taskToEdit!!.copy(
                        title = title,
                        noteText = desc,
                        priority = p,
                        tag = t,
                        dueDateLong = date
                    ))
                }
                showTaskDialog = false
            }
        )
    }

    // Note Create / Update dialog
    if (showNoteDialog) {
        NoteEditorDialog(
            note = noteToEdit,
            onDismiss = { showNoteDialog = false },
            onSave = { title, content, tag, colorHex, checklistText ->
                if (noteToEdit == null) {
                    viewModel.addNote(title, content, tag, colorHex, checklistText)
                } else {
                    viewModel.updateNote(noteToEdit!!.copy(
                        title = title,
                        content = content,
                        tag = tag,
                        colorHex = colorHex,
                        checklistText = checklistText
                    ))
                }
                showNoteDialog = false
            }
        )
    }

    // Gemini AI Copilot dialog state
    if (aiState !is AiState.Idle) {
        AiCopilotDialog(
            aiState = aiState,
            onDismiss = { viewModel.clearAiState() },
            onSyncTasks = { extractedTasks, tag ->
                viewModel.addAiGeneratedTasks(extractedTasks, tag)
                viewModel.clearAiState()
            }
        )
    }
}

// ================= COMPONENT FRAGMENTS =================

@Composable
fun CategoryFilterBar(
    selectedTag: String,
    onTagSelected: (String) -> Unit
) {
    val tags = listOf("All", "Work", "Personal", "Ideas", "Urgent")

    // Row is much simpler for horizontal tags bar:
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tags.forEach { tag ->
            val isSelected = tag == selectedTag
            FilterChip(
                selected = isSelected,
                onClick = { onTagSelected(tag) },
                label = { Text(tag) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("tag_filter_${tag.lowercase()}")
            )
        }
    }
}

@Composable
fun TasksWorkspace(
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyPlaceholder(
            icon = Icons.Default.CheckCircle,
            title = "Zero pending tasks found",
            subtitle = "Enjoy your day! Tap the FAB to create new todo schedules with priority metrics and dates."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { onToggleTask(task) },
                    onClick = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
            .combinedClickable(
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                if (task.noteText.isNotEmpty()) {
                    Text(
                        text = task.noteText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority tag
                    PriorityBadge(task.priority)
                    // Tag label
                    TagBadge(task.tag)
                    
                    if (task.dueDateLong > 0L) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendar",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        val dateString = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(task.dueDateLong))
                        Text(
                            text = dateString,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun NotesWorkspace(
    notes: List<Note>,
    onEditNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onTriggerAiSummary: (Note) -> Unit
) {
    if (notes.isEmpty()) {
        EmptyPlaceholder(
            icon = Icons.Default.Star,
            title = "Notes environment is empty",
            subtitle = "Tap the bottom-right action icon to add rich notes. Tap the AI Spark icon on notes to auto-summarize and generate checklist schedule tasks instantly!"
        )
    } else {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.screenWidthDp > 600
        val gridCells = if (isLandscape) GridCells.Fixed(3) else GridCells.Fixed(2)

        LazyVerticalGrid(
            columns = gridCells,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onEditNote(note) },
                    onDelete = { onDeleteNote(note) },
                    onTriggerAiSummary = { onTriggerAiSummary(note) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTriggerAiSummary: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val noteColors = getNoteCardColors(note.colorHex, isDark)
    
    // Calculate checklist information
    val subtasks = note.getSubtasks()
    val totalSubtasks = subtasks.size
    val completedSubtasks = subtasks.count { it.isChecked }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}")
            .combinedClickable(
                onClick = onClick
            ),
        colors = noteColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // AI Spark badge trigger if note content exists
                if (note.content.isNotBlank()) {
                    IconButton(
                        onClick = onTriggerAiSummary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Trigger AI Copywriter Summarize",
                            tint = if (isDark) Color(0xFFFFD700) else Color(0xFFC5A000),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(6.dp))

            Text(
                text = note.content,
                fontSize = 13.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Render Checklist stats if present
            if (totalSubtasks > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Checklists Status Icon",
                        modifier = Modifier.size(12.dp),
                        tint = noteColors.contentColor.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "✓ $completedSubtasks/$totalSubtasks tasks",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = noteColors.contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtle tag pill nested inside note card
                Box(
                    modifier = Modifier
                        .background(
                            color = noteColors.contentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = note.tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = noteColors.contentColor.copy(alpha = 0.9f)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = noteColors.contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (colorBg, colorText) = when (priority) {
        "High" -> Color(0xFFFFDAD9) to Color(0xFF410002)
        "Medium" -> Color(0xFFFFDDB3) to Color(0xFF291800)
        else -> Color(0xFFD2E8D4) to Color(0xFF0F2012)
    }
    Box(
        modifier = Modifier
            .background(colorBg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$priority priority",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colorText
        )
    }
}

@Composable
fun TagBadge(tag: String) {
    val (colorBg, colorText) = when (tag) {
        "Personal" -> Color(0xFFE0E7FF) to Color(0xFF3730A3)
        "Work" -> Color(0xFFF3E8FF) to Color(0xFF6B21A8)
        "Ideas" -> Color(0xFFCCFBF1) to Color(0xFF115E59)
        "Urgent" -> Color(0xFFFFE4E6) to Color(0xFF9F1239)
        else -> Color(0xFFF1F5F9) to Color(0xFF334155)
    }
    Box(
        modifier = Modifier
            .background(colorBg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = tag,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colorText
        )
    }
}

@Composable
fun getNoteCardColors(colorHex: String, isDark: Boolean): CardColors {
    val (bg, content) = when (colorHex) {
        "Slate" -> if (isDark) Color(0xFF1E293B) to Color(0xFFF1F5F9) else Color(0xFFF8FAFC) to Color(0xFF334155)
        "Blue" -> if (isDark) Color(0xFF1E3A8A) to Color(0xFFEFF6FF) else Color(0xFFDBEAFE) to Color(0xFF1E40AF)
        "Mint" -> if (isDark) Color(0xFF064E3B) to Color(0xFFECFDF5) else Color(0xFFD1FAE5) to Color(0xFF065F46)
        "Lavender" -> if (isDark) Color(0xFF4C1D95) to Color(0xFFF5F3FF) else Color(0xFFEDE9FE) to Color(0xFF6D28D9)
        "Peach" -> if (isDark) Color(0xFF7C2D12) to Color(0xFFFFF7ED) else Color(0xFFFFEDD5) to Color(0xFFC2410C)
        "Amber" -> if (isDark) Color(0xFF78350F) to Color(0xFFFFFBEB) else Color(0xFFFEF3C7) to Color(0xFFB45309)
        else -> if (isDark) Color(0xFF334155) to Color(0xFFF1F5F9) else Color(0xFFF1F5F9) to Color(0xFF334155)
    }
    return CardDefaults.cardColors(containerColor = bg, contentColor = content)
}

// ================= DIALOG BUILDERS & EDITORS =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, priority: String, tag: String, dueDate: Long) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var desc by remember { mutableStateOf(task?.noteText ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: "Medium") }
    var tag by remember { mutableStateOf(task?.tag ?: "Personal") }
    var dueDateLong by remember { mutableStateOf(task?.dueDateLong ?: 0L) }
    
    val context = LocalContext.current
    var isTitleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (task == null) "Create New Task" else "Edit Schedule Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        isTitleError = it.isBlank()
                    },
                    label = { Text("Task Title *") },
                    isError = isTitleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Task Note / Context") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Switcher (High, Medium, Low)
                Text("Select Priority Level", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("High", "Medium", "Low").forEach { level ->
                        val isSel = priority == level
                        OutlinedButton(
                            onClick = { priority = level },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Text(level, fontSize = 12.sp, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Tag Label Selector
                Text("Select Tag Label", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Work", "Personal", "Ideas", "Urgent").forEach { label ->
                        val isSel = tag == label
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { tag = label }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Due Date Select Block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            val cal = Calendar.getInstance()
                            if (dueDateLong > 0L) {
                                cal.timeInMillis = dueDateLong
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selCal = Calendar.getInstance()
                                    selCal.set(year, month, dayOfMonth)
                                    dueDateLong = selCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Due Date Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (dueDateLong == 0L) "No due date scheduled" else SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dueDateLong)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (dueDateLong > 0L) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { dueDateLong = 0L }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Due Date", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                isTitleError = true
                            } else {
                                onSave(title, desc, priority, tag, dueDateLong)
                            }
                        },
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
    note: Note?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, tag: String, colorHex: String, checklistText: String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var tag by remember { mutableStateOf(note?.tag ?: "Personal") }
    var colorHex by remember { mutableStateOf(note?.colorHex ?: "Slate") }
    
    // Checklist creation builder state
    val parsedSubtasks = remember { mutableStateListOf<Note.Subtask>().apply { addAll(note?.getSubtasks() ?: emptyList()) } }
    var newChecklistItemText by remember { mutableStateOf("") }

    var isTitleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (note == null) "Create Rich Note" else "Edit Note Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        isTitleError = it.isBlank()
                    },
                    label = { Text("Note Title *") },
                    isError = isTitleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content Body") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // Embedded Checklist Builder
                Text("Appended Checklist Subtasks", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                
                if (parsedSubtasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(parsedSubtasks.size) { index ->
                                val subtask = parsedSubtasks[index]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = subtask.isChecked,
                                        onCheckedChange = { isChecked ->
                                            parsedSubtasks[index] = subtask.copy(isChecked = isChecked)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = subtask.text,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { parsedSubtasks.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, "Remove subtask", Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newChecklistItemText,
                        onValueChange = { newChecklistItemText = it },
                        placeholder = { Text("new checklist item...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Button(
                        onClick = {
                            if (newChecklistItemText.isNotBlank()) {
                                parsedSubtasks.add(Note.Subtask(newChecklistItemText.trim(), false))
                                newChecklistItemText = ""
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add checklist")
                    }
                }

                // Custom Palette color dots
                Text("Select Ambient Theme Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf("Slate", "Blue", "Mint", "Lavender", "Peach", "Amber")
                    colors.forEach { colName ->
                        val (primaryColor, _) = when (colName) {
                            "Slate" -> Color(0xFF94A3B8) to Color(0xFF334155)
                            "Blue" -> Color(0xFF60A5FA) to Color(0xFF1E40AF)
                            "Mint" -> Color(0xFF34D399) to Color(0xFF065F46)
                            "Lavender" -> Color(0xFFA78BFA) to Color(0xFF6D28D9)
                            "Peach" -> Color(0xFFFDBA74) to Color(0xFFC2410C)
                            "Amber" -> Color(0xFFFBBF24) to Color(0xFFB45309)
                            else -> Color.Gray to Color.DarkGray
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                                .border(
                                    width = if (colorHex == colName) 3.dp else 0.dp,
                                    color = if (colorHex == colName) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = colName }
                        )
                    }
                }

                // Tag labels
                Text("Select Note Tag", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Work", "Personal", "Ideas", "Urgent").forEach { label ->
                        val isSel = tag == label
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { tag = label }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Save cancel actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                isTitleError = true
                            } else {
                                val checklistString = Note.serializeSubtasks(parsedSubtasks)
                                onSave(title, content, tag, colorHex, checklistString)
                            }
                        },
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text("Save Note")
                    }
                }
            }
        }
    }
}

@Composable
fun AiCopilotDialog(
    aiState: AiState,
    onDismiss: () -> Unit,
    onSyncTasks: (extractedTasks: List<String>, tag: String) -> Unit
) {
    Dialog(onDismissRequest = { if (aiState !is AiState.Loading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Spark",
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AI Note Copilot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                when (aiState) {
                    is AiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Gemini is scanning note topics...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Extracting key insights & suggested tasks",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    is AiState.Success -> {
                        // User-selected tasks state
                        val extractedTasks = aiState.tasks
                        val selectedExtractedTasks = remember { mutableStateListOf<String>().apply { addAll(extractedTasks) } }
                        var targetTag by remember { mutableStateOf("Ideas") }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Analyzing: \"${aiState.noteTitle}\"",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "AI Summary:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = aiState.summary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Extracted Action Tasks:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            if (extractedTasks.isEmpty()) {
                                item {
                                    Text(
                                        text = "No clear todo tasks found in this note text. Add action details to your notes to auto-extract items.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            } else {
                                items(extractedTasks) { itemText ->
                                    val isChecked = selectedExtractedTasks.contains(itemText)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) selectedExtractedTasks.remove(itemText)
                                                else selectedExtractedTasks.add(itemText)
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                if (it == true) {
                                                    selectedExtractedTasks.add(itemText)
                                                } else {
                                                    selectedExtractedTasks.remove(itemText)
                                                }
                                            }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(itemText, fontSize = 13.sp)
                                    }
                                }
                            }

                            item {
                                Spacer(Modifier.height(12.dp))
                                Text("Assign Extracted Tasks Tag", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Work", "Personal", "Ideas", "Urgent").forEach { label ->
                                        val isSel = targetTag == label
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { targetTag = label }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sync button actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Skip")
                            }
                            if (selectedExtractedTasks.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { onSyncTasks(selectedExtractedTasks.toList(), targetTag) }
                                ) {
                                    Icon(Icons.Default.Check, "Sync Icon")
                                    Spacer(Modifier.width(6.dp))
                                    Text("Sync to Todos (${selectedExtractedTasks.size})")
                                }
                            }
                        }
                    }
                    is AiState.Error -> {
                        Column {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error Info",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Analysis Integration Error",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = aiState.message,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(onClick = onDismiss) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
