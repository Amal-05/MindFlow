package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = DatabaseProvider.getRepository(this)
        val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }

        setContent {
            val isDarkTheme by viewModel.appThemeDark.collectAsStateWithLifecycle()
            var isAuthenticated by remember { mutableStateOf(false) }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isAuthenticated) {
                        AuthScreen(
                            viewModel = viewModel,
                            onAuthSuccess = { isAuthenticated = true }
                        )
                    } else {
                        AppLockWrapper(viewModel = viewModel) {
                            MainNavigationScreen(viewModel = viewModel)
                        }
                    }
                }
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

// --- App Lock Gatekeeper ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockWrapper(viewModel: MainViewModel, content: @Composable () -> Unit) {
    val lockPin by viewModel.appLockPin.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.appLockUnlocked.collectAsStateWithLifecycle()
    
    if (lockPin.isEmpty() || isUnlocked) {
        content()
    } else {
        var enteredPin by remember { mutableStateOf("") }
        var showPinError by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted Lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Biometric & Vault Lock",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                text = "Please enter your vault PIN to reveal secure checklists and records.",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Masked dots representation
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..4).forEach { index ->
                    val isFilled = enteredPin.length >= index
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            if (showPinError) {
                Text(
                    text = "Incorrect personal PIN code context. Try again.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // Traditional numeric security keypad
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(260.dp)
            ) {
                items(keys) { key ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                when (key) {
                                    "C" -> {
                                        enteredPin = ""
                                        showPinError = false
                                    }
                                    "OK" -> {
                                        if (enteredPin == lockPin) {
                                            viewModel.appLockUnlocked.value = true
                                        } else {
                                            showPinError = true
                                            enteredPin = ""
                                        }
                                    }
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// --- Navigation Layout Center ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(viewModel: MainViewModel) {
    var navigationIndex by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Dialog triggers
    var showTaskCreator by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showNoteCreator by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var showHabitCreator by remember { mutableStateOf(false) }

    // Collect current values
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    
    // Profile metadata
    val pName by viewModel.userProfileName.collectAsStateWithLifecycle()
    val pBio by viewModel.userProfileBio.collectAsStateWithLifecycle()
    val pEmoji by viewModel.userProfileEmoji.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pEmoji, fontSize = 20.sp)
                            }
                            Column {
                                Text(pName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(pBio, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Text("💼 WORKSPACE MODULES", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 8.dp))
                    
                    val drawerItems = listOf(
                        "Dashboard Panel" to Icons.Default.Home,
                        "Advanced Tasks" to Icons.Default.CheckCircle,
                        "Notes & Sketch Pad" to Icons.Default.Edit,
                        "Daily Habits Hub" to Icons.Default.Refresh,
                        "Kanban Workflow" to Icons.Default.Build,
                        "Class Study Assistant" to Icons.Default.Star,
                        "Folders & Collab" to Icons.Default.Share,
                        "Interactive Sketch Canvas" to Icons.Default.Create,
                        "Vocal Transcriber" to Icons.Default.PlayArrow,
                        "OCR Digitizer Scanner" to Icons.Default.Search,
                        "Emotional Life Plan" to Icons.Default.Favorite,
                        "Security & Settings" to Icons.Default.Settings
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(drawerItems) { idx, item ->
                            val isSelected = idx == navigationIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        navigationIndex = idx
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.second,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = item.first,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Secure cloud synchronizer verified", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (navigationIndex) {
                                0 -> "Infinity Dashboard"
                                1 -> "Task Directives"
                                2 -> "Workspace Notebook"
                                3 -> "Daily Streak Rituals"
                                4 -> "Pipeline Kanban Board"
                                5 -> "Active Recall Study"
                                6 -> "Collab Folder Sync"
                                7 -> "Interactive Sketch Canvas"
                                8 -> "AI Speech Transcriber"
                                9 -> "OCR Document Digitizer"
                                10 -> "Executive Life Coach"
                                11 -> "Security & Preferences"
                                else -> "Workspace"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Switch Toggle")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (navigationIndex) {
                    0 -> DashboardWorkspace(
                        viewModel = viewModel,
                        tasks = tasks,
                        notes = notes,
                        habits = habits,
                        onNavigateToTab = { index -> navigationIndex = index },
                        onAddTaskQuick = {
                            taskToEdit = null
                            showTaskCreator = true
                        },
                        onAddNoteQuick = {
                            noteToEdit = null
                            showNoteCreator = true
                        },
                        onRecordVoiceQuick = { navigationIndex = 8 },
                        onDrawPadQuick = { navigationIndex = 7 },
                        onOcrScannerQuick = { navigationIndex = 9 }
                    )
                    1 -> TasksWorkspace(
                        viewModel = viewModel,
                        tasks = tasks,
                        onAddTask = {
                            taskToEdit = null
                            showTaskCreator = true
                        },
                        onEditTask = {
                            taskToEdit = it
                            showTaskCreator = true
                        }
                    )
                    2 -> NotesWorkspace(
                        viewModel = viewModel,
                        notes = notes,
                        onAddNote = {
                            noteToEdit = null
                            showNoteCreator = true
                        },
                        onEditNote = {
                            noteToEdit = it
                            showNoteCreator = true
                        },
                        onDrawNotes = { navigationIndex = 7 }
                    )
                    3 -> DailyHubWorkspace(
                        viewModel = viewModel,
                        habits = habits,
                        tasks = tasks,
                        onAddHabit = { showHabitCreator = true }
                    )
                    4 -> KanbanBoardScreen(
                        viewModel = viewModel,
                        tasks = tasks,
                        onSelectTask = {
                            taskToEdit = it
                            showTaskCreator = true
                        },
                        onAddTaskDirectly = { _ ->
                            taskToEdit = null
                            showTaskCreator = true
                        }
                    )
                    5 -> StudyAssistantScreen(
                        viewModel = viewModel,
                        notes = notes
                    )
                    6 -> FolderCollabScreen(
                        viewModel = viewModel,
                        notes = notes,
                        onSelectNote = {
                            noteToEdit = it
                            showNoteCreator = true
                        }
                    )
                    7 -> DrawingWorkspace(
                        viewModel = viewModel,
                        onNavigateBack = { navigationIndex = 2 }
                    )
                    8 -> VoiceAssistantWorkspace(
                        viewModel = viewModel,
                        onNavigateBack = { navigationIndex = 0 }
                    )
                    9 -> OcrScannerWorkspace(
                        viewModel = viewModel,
                        onNavigateBack = { navigationIndex = 0 }
                    )
                    10 -> LifeDashboardScreen(
                        viewModel = viewModel,
                        tasks = tasks,
                        notes = notes
                    )
                    11 -> PreferencesWorkspace(
                        viewModel = viewModel,
                        tasks = tasks,
                        notes = notes
                    )
                }
            }
        }
    }

    // Task Dialog Control
    if (showTaskCreator) {
        TaskEditorDialog(
            task = taskToEdit,
            onDismiss = { showTaskCreator = false },
            onSave = { title, desc, priority, tag, category, dueDate, dueTime, status, subtasks, recurring, priorityTime, remindOption, depId ->
                if (taskToEdit == null) {
                    viewModel.addTask(
                        title = title,
                        description = desc,
                        priority = priority,
                        tag = tag,
                        category = category,
                        dueDateLong = dueDate,
                        dueTime = dueTime,
                        status = status,
                        subtasksText = subtasks,
                        recurringOption = recurring,
                        estimatedTimeMinutes = priorityTime,
                        reminderMinutesBefore = remindOption,
                        dependencyTaskId = depId
                    )
                } else {
                    viewModel.updateTask(taskToEdit!!.copy(
                        title = title,
                        noteText = desc,
                        priority = priority,
                        tag = tag,
                        category = category,
                        dueDateLong = dueDate,
                        dueTime = dueTime,
                        status = status,
                        subtasksText = subtasks,
                        recurringOption = recurring,
                        estimatedTimeMinutes = priorityTime,
                        reminderMinutesBefore = remindOption,
                        dependencyTaskId = depId
                    ))
                }
                showTaskCreator = false
            }
        )
    }

    // Note Dialog Control
    if (showNoteCreator) {
        NoteEditorDialog(
            note = noteToEdit,
            onDismiss = { showNoteCreator = false },
            onSave = { title, text, tag, colorHex, checklist ->
                if (noteToEdit == null) {
                    viewModel.addNote(title, text, tag, colorHex, checklist)
                } else {
                    viewModel.updateNote(noteToEdit!!.copy(
                        title = title,
                        content = text,
                        tag = tag,
                        colorHex = colorHex,
                        checklistText = checklist
                    ))
                }
                showNoteCreator = false
            }
        )
    }

    // Habit Create Dialog
    if (showHabitCreator) {
        HabitCreatorDialog(
            onDismiss = { showHabitCreator = false },
            onSave = { name, cat, time ->
                viewModel.addHabit(name, cat, time)
                showHabitCreator = false
            }
        )
    }

    // AI State Copilot Dialog
    if (aiState !is AiState.Idle) {
        AiResponseDialog(
            aiState = aiState,
            onDismiss = { viewModel.clearAiState() },
            onImportTasks = { items ->
                viewModel.addAiGeneratedTasks(items, "Ideas")
                viewModel.clearAiState()
            }
        )
    }
}

// ==========================================
// 1. DASHBOARD WORKSPACE (HOME DISPLAY)
// ==========================================
@Composable
fun DashboardWorkspace(
    viewModel: MainViewModel,
    tasks: List<Task>,
    notes: List<Note>,
    habits: List<Habit>,
    onNavigateToTab: (Int) -> Unit,
    onAddTaskQuick: () -> Unit,
    onAddNoteQuick: () -> Unit,
    onRecordVoiceQuick: () -> Unit,
    onDrawPadQuick: () -> Unit,
    onOcrScannerQuick: () -> Unit
) {
    val context = LocalContext.current
    val userProfileName by viewModel.userProfileName.collectAsStateWithLifecycle()
    val userProfileEmoji by viewModel.userProfileEmoji.collectAsStateWithLifecycle()

    val pendingTasks = tasks.filter { !it.isCompleted }
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size
    val dailyProgress = if (totalCount == 0) 100 else (completedCount * 100) / totalCount
    
    // Current live calendar time representation
    val formatDay = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
    var liveSecondsText by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        while (true) {
            liveSecondsText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Welcoming card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Welcome back, $userProfileEmoji",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = userProfileName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        // Live clock badge
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = liveSecondsText,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "📅 $formatDay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    
                    // Unified Life Productivity Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Productivity Rate",
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$dailyProgress%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { dailyProgress.toFloat() / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Quick Actions Dashboard Section
        item {
            Text(
                text = "⚡ Real-Time Quick Modules",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    QuickActionPill(
                        icon = Icons.Default.Add,
                        label = "Add Task",
                        bg = MaterialTheme.colorScheme.primaryContainer,
                        onClick = onAddTaskQuick
                    )
                }
                item {
                    QuickActionPill(
                        icon = Icons.Default.Edit,
                        label = "Rich Note",
                        bg = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onAddNoteQuick
                    )
                }
                item {
                    QuickActionPill(
                        icon = Icons.Default.KeyboardArrowUp,
                        label = "Voice Memo",
                        bg = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onRecordVoiceQuick
                    )
                }
                item {
                    QuickActionPill(
                        icon = Icons.Default.Face,
                        label = "Sketch Pad",
                        bg = Color(0xFFFFECE0),
                        onClick = onDrawPadQuick
                    )
                }
                item {
                    QuickActionPill(
                        icon = Icons.Default.Search,
                        label = "OCR Scan",
                        bg = Color(0xFFE2F9E8),
                        onClick = onOcrScannerQuick
                    )
                }
            }
        }

        // Eisenhower Fast Matrix Checkup
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(1) }, // Navigation tab inside Todos
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 Priority Matrix (Eisenhower)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Icon(Icons.Default.ArrowForward, contentDescription = "Open matrix", modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFFFDAD9), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Urgent & Import", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF410002))
                                Text("${tasks.count { it.priority == "Critical" && !it.isCompleted }} Active", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFFFECCC), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Import - Non Urgent", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5E2B00))
                                Text("${tasks.count { it.priority == "High" && !it.isCompleted }} Active", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // Recent Tasks Overview list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔔 Imminent Critical Deadlines",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                TextButton(onClick = { onNavigateToTab(1) }) {
                    Text("View all", fontSize = 12.sp)
                }
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nice job! You have zero outstanding schedules.",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(pendingTasks.take(3)) { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (task.priority) {
                                    "Critical" -> Color.Red
                                    "High" -> Color.Magenta
                                    "Medium" -> Color.Yellow
                                    else -> Color.Green
                                }
                            )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${task.category} • Due: ${task.dueTime}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    // Simple progress percentage badge on dashboard
                    if (task.subtasksText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Progress: ${task.getProgressPercentage()}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Recent Notes Display
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 Saved Notes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                TextButton(onClick = { onNavigateToTab(2) }) {
                    Text("Browse notes", fontSize = 12.sp)
                }
            }
        }

        if (notes.isEmpty()) {
            item {
                Text(
                    text = "No notes created yet. Spark your intelligence using the Notes tab.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes.take(4)) { note ->
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (note.colorHex) {
                                        "Blue" -> Color(0xFFDBEAFE).copy(alpha = 0.8f)
                                        "Mint" -> Color(0xFFD1FAE5).copy(alpha = 0.8f)
                                        "Lavender" -> Color(0xFFEDE9FE).copy(alpha = 0.8f)
                                        "Peach" -> Color(0xFFFFEDD5).copy(alpha = 0.8f)
                                        "Amber" -> Color(0xFFFEF3C7).copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { onNavigateToTab(2) }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = note.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = note.content,
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionPill(
    icon: ImageVector,
    label: String,
    bg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.Black, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

// ==========================================
// 2. ADVANCED TASK MANAGEMENT (TODOS GRID)
// ==========================================
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
                                Text("In Progress Schedules", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
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
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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

// 2.a INTERACTIVE CALENDAR BOARD FRAGMENT
@Composable
fun CalendarInteractiveView(tasks: List<Task>, onSelectTask: (Task) -> Unit) {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    
    // Simple calendar layout starting this week
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

        // filter tasks matching selected date (using simple format matcher)
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

// 2.b EISENHOWER DECISION QUADRANTS MATRIX
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

// ==========================================
// 3. NOTE TAKING WORKSPACE
// ==========================================
@Composable
fun NotesWorkspace(
    viewModel: MainViewModel,
    notes: List<Note>,
    onAddNote: () -> Unit,
    onEditNote: (Note) -> Unit,
    onDrawNotes: () -> Unit
) {
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Creative Canvas", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Row {
                IconButton(onClick = onDrawNotes) {
                    Icon(Icons.Default.Face, contentDescription = "Draw pad sketch", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Horizontal tag filters
        CategorySelectorsBar(
            selectedTag = selectedTag,
            onTagSelected = { viewModel.setTag(it) }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (notes.isEmpty()) {
                EmptyPlaceholder(
                    icon = Icons.Default.Edit,
                    title = "Your Knowledge Base is blank",
                    subtitle = "Store snippets, sketch ideas, and activate Gemini AI summaries to extract milestones in seconds."
                )
            } else {
                val isLandscape = LocalConfiguration.current.screenWidthDp > 600
                val cols = if (isLandscape) 3 else 2
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes) { note ->
                        AdvancedNoteCard(
                            note = note,
                            onClick = { onEditNote(note) },
                            onDelete = { viewModel.deleteNote(note) },
                            onTriggerAi = { viewModel.summarizeAndGenerateTasksFromNote(note) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onAddNote,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
                Spacer(Modifier.width(6.dp))
                Text("Draft Note", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvancedNoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTriggerAi: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val noteColors = getNoteCardColors(note.colorHex, isDark)
    val parsedSubtasks = note.getSubtasks()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        colors = noteColors,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (note.content.isNotEmpty()) {
                    IconButton(
                        onClick = onTriggerAi,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Gemini Spark",
                            tint = if (isDark) Color(0xFFFFD700) else Color(0xFFC5A000)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(6.dp))
            Text(
                text = note.content,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Render miniature checklist counts if present
            if (parsedSubtasks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val complete = parsedSubtasks.count { it.isChecked }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$complete/${parsedSubtasks.size} subtasks", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(noteColors.contentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(note.tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = noteColors.contentColor)
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Drop Note",
                        tint = noteColors.contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. DAILY PRODUCTIVITY HUB (HABITS & FOCUS)
// ==========================================
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
    val breakLength by viewModel.pomodoroBreakIntervalMinutes.collectAsStateWithLifecycle()
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
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isFocusActive) "Session Active" else "Paused",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.focusSessionActive.value = !isFocusActive },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFocusActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isFocusActive) "Pause Timer" else "Start Session")
            }

            OutlinedButton(
                onClick = {
                    viewModel.focusSessionActive.value = false
                    viewModel.focusDurationCompletedSeconds.value = 0
                }
            ) {
                Text("Reset Focus")
            }
        }

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // Duration Adjustment Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Adjust Focus Duration (Minutes):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.pomodoroWorkIntervalMinutes.value = maxOf(5, workLength - 5) }) {
                    Icon(Icons.Default.Clear, contentDescription = "sub")
                }
                Text("$workLength min", fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = { viewModel.pomodoroWorkIntervalMinutes.value = workLength + 5 }) {
                    Icon(Icons.Default.Add, contentDescription = "add")
                }
            }
        }
    }
}

// ==========================================
// 5. CONTROL AND PREFERENCES WORKSPACE
// ==========================================
@Composable
fun ControlAndPreferencesWorkspace(
    viewModel: MainViewModel,
    tasks: List<Task>,
    notes: List<Note>
) {
    var pName by remember { mutableStateOf(viewModel.userProfileName.value) }
    var pBio by remember { mutableStateOf(viewModel.userProfileBio.value) }
    var pEmoji by remember { mutableStateOf(viewModel.userProfileEmoji.value) }
    val isDarkTh by viewModel.appThemeDark.collectAsStateWithLifecycle()
    var pinCodeLock by remember { mutableStateOf(viewModel.appLockPin.value) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("User Configuration & Security", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("Setup sync channels, lock PIN values, and AI features reports.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }

        // Profile Form edit card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Personal Identity Credentials", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("Display Label Profile Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pBio,
                        onValueChange = { pBio = it },
                        label = { Text("Profile status bio") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pEmoji,
                        onValueChange = { pEmoji = it },
                        label = { Text("Status Avatar Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // App locks & Vault code parameters
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🔒 Security Vault Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Enter a 4-digit code below to restrict workspace entrance upon launch.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    
                    OutlinedTextField(
                        value = pinCodeLock,
                        onValueChange = { pinCodeLock = it.take(4) },
                        label = { Text("Vault Entry PIN (4 digits)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Theme toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preferred Obsidian Dark Theme", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isDarkTh,
                            onCheckedChange = { viewModel.appThemeDark.value = it }
                        )
                    }
                }
            }
        }

        // Action Trigger Button Group
        item {
            Button(
                onClick = {
                    viewModel.saveProfile(pName, pBio, pEmoji, isDarkTh, pinCodeLock)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lock and Save Parameters")
            }
        }

        // AI COACHING REPORT TRIGGERS
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📈 AI Productivity Diagnostic Specialist", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Request Gemini to parse all categories logs, deadlines performance, and give expert feedback insights.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    
                    Button(
                        onClick = {
                            viewModel.runProductivityAnalysis(tasks, tasks.count { it.isCompleted })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Draft Productivity Coaching Review")
                    }
                }
            }
        }
    }
}

// ==========================================
// MOCK / UTILS OR DIALOG DIALOG CONTROLLERS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        desc: String,
        priority: String,
        tag: String,
        category: String,
        dueDate: Long,
        dueTime: String,
        status: String,
        subtasks: String,
        recurring: String,
        priorityTime: Int,
        remindOption: Int,
        depId: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var desc by remember { mutableStateOf(task?.noteText ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: "Medium") }
    var tag by remember { mutableStateOf(task?.tag ?: "Personal") }
    var category by remember { mutableStateOf(task?.category ?: "Personal") }
    var dueDateLong by remember { mutableStateOf(task?.dueDateLong ?: 0L) }
    var dueTime by remember { mutableStateOf(task?.dueTime ?: "12:00 PM") }
    var status by remember { mutableStateOf(task?.status ?: "Pending") }
    var recurringOption by remember { mutableStateOf(task?.recurringOption ?: "None") }
    var estimatedTime by remember { mutableStateOf(task?.estimatedTimeMinutes ?: 30) }
    var remindBeforeMinutes by remember { mutableStateOf(task?.reminderMinutesBefore ?: -1) }

    // Subtask lists support within Task Dialog builder
    val listItems = remember { mutableStateListOf<Task.Subtask>().apply { addAll(task?.getSubtasks() ?: emptyList()) } }
    var newSubtaskName by remember { mutableStateOf("") }

    val context = LocalContext.current
    var isTitleErr by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (task == null) "Add Advanced Task" else "Update Milestone Info",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Divider()

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        isTitleErr = it.isBlank()
                    },
                    label = { Text("Task Title *") },
                    isError = isTitleErr,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description Note / Action Items") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Priorities
                Text("Priority Level Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Low", "Medium", "High", "Critical").forEach { level ->
                        val sel = priority == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { priority = level }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                level,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Choose Class Category
                Text("Workspace Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Personal", "Work", "Study", "Shopping", "Health", "Finance").forEach { cat ->
                        val sel = category == cat
                        FilterChip(
                            selected = sel,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Choose Class labels (Tags)
                Text("Class Status Labels / Tags", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Urgent", "Fam", "Project", "Family").forEach { label ->
                        val sel = tag == label
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { tag = label }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Time Tracking Estimated Time Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time tracking estimate minutes:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { estimatedTime = maxOf(10, estimatedTime - 10) }) {
                            Icon(Icons.Default.Clear, "sub")
                        }
                        Text("$estimatedTime min", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { estimatedTime += 10 }) {
                            Icon(Icons.Default.Add, "add")
                        }
                    }
                }

                // Recurring selector
                Text("Recurring Interval Config", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("None", "Daily", "Weekly", "Monthly").forEach { op ->
                        val sel = recurringOption == op
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { recurringOption = op }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                op,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Advanced Due Date Clock picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .clickable {
                            val c = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val sel = Calendar.getInstance()
                                    sel.set(y, m, d)
                                    dueDateLong = sel.timeInMillis
                                    
                                    // Instantly show time picker
                                    TimePickerDialog(
                                        context,
                                        { _, hour, min ->
                                            val ampm = if (hour >= 12) "PM" else "AM"
                                            val hourFmt = if (hour % 12 == 0) 12 else hour % 12
                                            dueTime = String.format("%02d:%02d %s", hourFmt, min, ampm)
                                        },
                                        12, 0, false
                                    ).show()
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Date", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Interactive Date Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (dueDateLong == 0L) "No deadline assigned"
                            else SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dueDateLong)) + " @ $dueTime",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Nested subtask item checklist creation
                Spacer(Modifier.height(8.dp))
                Text("Manage Nested Subtasks checklist", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                if (listItems.isNotEmpty()) {
                    listItems.forEachIndexed { index, subtask ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = subtask.isChecked, onCheckedChange = { listItems[index] = subtask.copy(isChecked = it) })
                            Text(subtask.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { listItems.removeAt(index) }) {
                                Icon(Icons.Default.Clear, contentDescription = "drop subtask")
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSubtaskName,
                        onValueChange = { newSubtaskName = it },
                        label = { Text("Draft quick checklist component") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newSubtaskName.isNotBlank()) {
                                listItems.add(Task.Subtask(newSubtaskName.trim(), false))
                                newSubtaskName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "add subtask")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            isTitleErr = true
                        } else {
                            val serialized = Task.serializeSubtasks(listItems)
                            onSave(
                                title,
                                desc,
                                priority,
                                tag,
                                category,
                                dueDateLong,
                                dueTime,
                                status,
                                serialized,
                                recurringOption,
                                estimatedTime,
                                remindBeforeMinutes,
                                0
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_task_button")
                ) {
                    Text("Save Milestone Info")
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
    onSave: (title: String, content: String, tag: String, color: String, checklist: String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var tag by remember { mutableStateOf(note?.tag ?: "Personal") }
    var color by remember { mutableStateOf(note?.colorHex ?: "Slate") }
    
    val checklistItems = remember { mutableStateListOf<Note.Subtask>().apply { addAll(note?.getSubtasks() ?: emptyList()) } }
    var newCheckItem by remember { mutableStateOf("") }
    var isTitleErr by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (note == null) "Compose Rich Note" else "Modify Note Settings",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Divider()

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        isTitleErr = it.isBlank()
                    },
                    label = { Text("Note Title *") },
                    isError = isTitleErr,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Write content (Markdown headings / links supported)") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 10
                )

                // Select card colors
                Text("Select Ambient Palette", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
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
                                    width = if (color == colName) 3.dp else 0.dp,
                                    color = if (color == colName) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = colName }
                        )
                    }
                }

                // Choose label tag
                Text("Select Tag Label", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Work", "Personal", "Ideas", "Urgent").forEach { label ->
                        val sel = tag == label
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { tag = label }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (sel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // checklist manager inside notes editor is extremely powerful
                Text("Manage Appended Checklist Items", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                
                checklistItems.forEachIndexed { index, checkItem ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checkItem.isChecked, onCheckedChange = { checklistItems[index] = checkItem.copy(isChecked = it) })
                        Text(checkItem.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { checklistItems.removeAt(index) }) {
                            Icon(Icons.Default.Clear, contentDescription = "drop checklist item")
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCheckItem,
                        onValueChange = { newCheckItem = it },
                        label = { Text("Draft check list item") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newCheckItem.isNotBlank()) {
                                checklistItems.add(Note.Subtask(newCheckItem.trim(), false))
                                newCheckItem = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "add checklist items")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            isTitleErr = true
                        } else {
                            val serialized = Note.serializeSubtasks(checklistItems)
                            onSave(title, content, tag, color, serialized)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_note_button")
                ) {
                    Text("Save Content Elements")
                }
            }
        }
    }
}

@Composable
fun HabitCreatorDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, time: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Health") }
    var time by remember { mutableStateOf("08:00 AM") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Build Daily Habit Tracker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Action Metric Name (e.g., Exercise)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Habit Class Sector", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Health", "Mind", "Study", "Work").forEach { cat ->
                        val sel = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { category = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Time picker triggers
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Trigger time alarm") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hr, min ->
                                        val ampm = if (hr >= 12) "PM" else "AM"
                                        val hrNormalized = if (hr % 12 == 0) 12 else hr % 12
                                        time = String.format("%02d:%02d %s", hrNormalized, min, ampm)
                                    },
                                    8, 0, false
                                ).show()
                            }
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick alarm time")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), category, time)
                    }
                }
            ) { Text("Create Habit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    )
}

// ----------------------------------------------------
// 5. DRAWING CANVAS DRAWPAD DIALOG COMPONENT
// ----------------------------------------------------
@Composable
fun DrawingCanvasDialog(
    onDismiss: () -> Unit,
    onSaveCanvas: (title: String, payload: String) -> Unit
) {
    var sketchName by remember { mutableStateOf("Creative Blueprint") }
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Interactive Sketch Canvas Pad", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                
                OutlinedTextField(
                    value = sketchName,
                    onValueChange = { sketchName = it },
                    label = { Text("Canvas Label Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // The whiteboard canvas element
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = newPath
                                    paths.add(newPath)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    // Trigger recomposition explicitly by removing and adding path
                                    if (currentPath != null) {
                                        val idx = paths.size - 1
                                        if (idx >= 0) {
                                            paths[idx] = currentPath!!
                                        }
                                    }
                                },
                                onDragEnd = {
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { path ->
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(width = 6f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { paths.clear() }) { Text("Clear Canvas") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val stringPayload = "Scribbled canvas drawing blueprint saved containing ${paths.size} vector paths strokes."
                            onSaveCanvas(sketchName, stringPayload)
                        }
                    ) {
                        Text("Save Drawing Notes")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. VOICE SIMULATED COMMAND PARSER DIALOG
// ----------------------------------------------------
@Composable
fun VoiceCommandDialog(
    onDismiss: () -> Unit,
    onExecuteVoiceCommand: (phrase: String) -> Unit
) {
    var rawPhrase by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.Red)
                Spacer(Modifier.width(8.dp))
                Text("AI Speech-to-Text & Command Line")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Speak or enter natural language commands to automatically format tasks or thoughts.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                
                OutlinedTextField(
                    value = rawPhrase,
                    onValueChange = { rawPhrase = it },
                    placeholder = { Text("Example: 'Create task Buy groceries tomorrow'") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        isRecording = !isRecording
                        if (isRecording) {
                            rawPhrase = "Create task Submit final year engineering presentation tomorrow"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isRecording) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRecording) "🛑 Mic active... click to transcribe mock" else "🎙️ Start Voice Command Mic Input")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawPhrase.isNotBlank()) {
                        onExecuteVoiceCommand(rawPhrase)
                    }
                }
            ) { Text("Execute Command") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ----------------------------------------------------
// 7. OCR DOCUMENT SCANNER MOCK DIALOG
// ----------------------------------------------------
@Composable
fun OcrScannerDialog(
    onDismiss: () -> Unit,
    onSaveOcrText: (title: String, payload: String) -> Unit
) {
    var payloadText by remember { mutableStateOf("Processing scanner...") }
    var isScanningMode by remember { mutableStateOf(true) }

    LaunchedEffect(isScanningMode) {
        if (isScanningMode) {
            delay(2000)
            payloadText = """
                SYSTEM LOGS OR SCAN RESULT:
                - Target title: Machine Learning lecture syllabus
                - Core tasks identified:
                  1. Download datasets from Keggle.
                  2. Construct regression modeling weights.
                  3. Send thesis outline before Sunday noon.
            """.trimIndent()
            isScanningMode = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OCR Handwritten Scanned Snippets", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                if (isScanningMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.Black, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text("Analyzing target print coordinates...", color = Color.White, fontSize = 12.sp)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = payloadText,
                        onValueChange = { payloadText = it },
                        label = { Text("OCR Transcribbed coordinates payload") },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        maxLines = 8
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveOcrText("OCR Scanned Topic", payloadText)
                        },
                        enabled = !isScanningMode
                    ) {
                        Text("Save Scanned Content")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 8. AI STATE RESPONSE MODALS
// ----------------------------------------------------
@Composable
fun AiResponseDialog(
    aiState: AiState,
    onDismiss: () -> Unit,
    onImportTasks: (List<String>) -> Unit
) {
    Dialog(onDismissRequest = { if (aiState !is AiState.Loading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gemini AI Copilot Analyzer", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }

                Divider()

                when (aiState) {
                    is AiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Analyzing semantic variables...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Connecting securely to Gemini LLM endpoint", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    is AiState.Success -> {
                        Text("Note Summarization Report:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        Text(aiState.summary, fontSize = 14.sp, lineHeight = 20.sp)
                        
                        if (aiState.tasks.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Extracted Action Milestones:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            aiState.tasks.forEach { t ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(t, fontSize = 13.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { onImportTasks(aiState.tasks) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Import extracted items into Tasks list")
                            }
                        }
                    }
                    is AiState.WritingAssistantSuccess -> {
                        Text("Original Draft Text:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(aiState.originalText, fontSize = 13.sp)

                        Spacer(Modifier.height(8.dp))
                        Text("Gemini Proposed Rewrite Response:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text(aiState.revisedText, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                    }
                    is AiState.PlannerSuccess -> {
                        Text("Generative Daily Planner schedule agenda:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(aiState.formattedSchedule, fontSize = 14.sp, fontFamily = FontFamily.Monospace, lineHeight = 20.sp)
                    }
                    is AiState.ProductivityAnalysis -> {
                        Text("User Productivity Rating Score: ${aiState.score}/100", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Personal coaching insights analysis:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(aiState.insights, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                    is AiState.Error -> {
                        Text("Copilot encountered a problem context:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(aiState.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }

                if (aiState !is AiState.Loading) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss Report")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(icon: ImageVector, title: String, subtitle: String) {
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
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun getNoteCardColors(colorHex: String, isDark: Boolean): CardColors {
    val (bg, content) = when (colorHex) {
        "Slate" -> if (isDark) Color(0xFF1E293B) to Color(0xFFF1F5F9) else Color(0xFFF8FAFC) to Color(0xFF334155)
        "Blue" -> if (isDark) Color(0xFF1E3A8A) to Color(0xFFDBEAFE) else Color(0xFFEFF6FF) to Color(0xFF1E40AF)
        "Mint" -> if (isDark) Color(0xFF064E3B) to Color(0xFFD1FAE5) else Color(0xFFECFDF5) to Color(0xFF065F46)
        "Lavender" -> if (isDark) Color(0xFF3B0764) to Color(0xFFEDE9FE) else Color(0xFFF5F3FF) to Color(0xFF6D28D9)
        "Peach" -> if (isDark) Color(0xFF7C2D12) to Color(0xFFFFEDD5) else Color(0xFFFFF7ED) to Color(0xFFC2410C)
        "Amber" -> if (isDark) Color(0xFF78350F) to Color(0xFFFEF3C7) else Color(0xFFFFFBEB) to Color(0xFFB45309)
        else -> if (isDark) Color(0xFF1E293B) to Color(0xFFF1F5F9) else Color(0xFFF8FAFC) to Color(0xFF334155)
    }
    return CardDefaults.cardColors(containerColor = bg, contentColor = content)
}
