package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed interface AiState {
    object Idle : AiState
    object Loading : AiState
    data class Success(val summary: String, val tasks: List<String>, val noteTitle: String) : AiState
    data class WritingAssistantSuccess(val originalText: String, val revisedText: String) : AiState
    data class PlannerSuccess(val formattedSchedule: String) : AiState
    data class ProductivityAnalysis(val score: Int, val insights: String) : AiState
    data class Error(val message: String) : AiState
}

class MainViewModel(private val repository: TaskAndNoteRepository) : ViewModel() {

    val selectedTag = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")
    
    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    // --- Tab filters and query bindings ---
    val filteredNotes: StateFlow<List<Note>> = combine(
        repository.allNotes,
        selectedTag,
        searchQuery
    ) { notes, tag, query ->
        notes.filter { note ->
            val matchesTag = (tag == "All" || note.tag.equals(tag, ignoreCase = true))
            val matchesQuery = query.isEmpty() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.checklistText.contains(query, ignoreCase = true)
            matchesTag && matchesQuery
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val filteredTasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        selectedTag,
        searchQuery
    ) { tasks, tag, query ->
        tasks.filter { task ->
            val matchesTag = (tag == "All" || task.tag.equals(tag, ignoreCase = true))
            val matchesQuery = query.isEmpty() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.noteText.contains(query, ignoreCase = true)
            matchesTag && matchesQuery
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val allHabits: StateFlow<List<Habit>> = repository.allHabits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Settings and state configurations ---
    val appThemeDark = MutableStateFlow(true)
    val appLockPin = MutableStateFlow("")  // Empty means no PIN lock active
    val appLockUnlocked = MutableStateFlow(true)
    val userProfileName = MutableStateFlow("Creative Genius")
    val userProfileBio = MutableStateFlow("Designing the future, one task at a time.")
    val userProfileEmoji = MutableStateFlow("🚀")
    val notifyIntervalMinutes = MutableStateFlow(30)

    // --- Active Pomodoro Timer / Focus States ---
    val focusSessionActive = MutableStateFlow(false)
    val focusDurationCompletedSeconds = MutableStateFlow(0)
    val pomodoroWorkIntervalMinutes = MutableStateFlow(25)
    val pomodoroBreakIntervalMinutes = MutableStateFlow(5)

    fun setTag(tag: String) {
        selectedTag.value = tag
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    // --- Task CRUD operations (Expanded) ---
    fun addTask(
        title: String,
        description: String,
        priority: String,
        tag: String,
        category: String,
        dueDateLong: Long,
        dueTime: String,
        status: String = "Pending",
        subtasksText: String = "",
        recurringOption: String = "None",
        estimatedTimeMinutes: Int = 30,
        reminderMinutesBefore: Int = -1,
        dependencyTaskId: Int = 0
    ) {
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    title = title,
                    noteText = description,
                    priority = priority,
                    tag = tag,
                    category = category,
                    dueDateLong = dueDateLong,
                    dueTime = dueTime,
                    status = status,
                    subtasksText = subtasksText,
                    recurringOption = recurringOption,
                    estimatedTimeMinutes = estimatedTimeMinutes,
                    reminderMinutesBefore = reminderMinutesBefore,
                    dependencyTaskId = dependencyTaskId
                )
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val nextCompleted = !task.isCompleted
            val nextStatus = if (nextCompleted) "Completed" else "Pending"
            repository.insertTask(task.copy(isCompleted = nextCompleted, status = nextStatus))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // --- Note CRUD operations (Expanded) ---
    fun addNote(title: String, content: String, tag: String, colorHex: String, checklistText: String) {
        viewModelScope.launch {
            repository.insertNote(
                Note(
                    title = title,
                    content = content,
                    tag = tag,
                    colorHex = colorHex,
                    checklistText = checklistText
                )
            )
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // --- Habit CRUD operations ---
    fun addHabit(name: String, category: String, triggerTime: String = "08:00 AM") {
        viewModelScope.launch {
            repository.insertHabit(
                Habit(
                    name = name,
                    category = category,
                    triggerTime = triggerTime
                )
            )
        }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayDate = simpleDateFormat.format(Date())
            val currentlyCompleted = habit.isCompletedToday
            val nextCompleted = !currentlyCompleted
            val newStreak = if (nextCompleted) {
                habit.streak + 1
            } else {
                maxOf(0, habit.streak - 1)
            }
            repository.updateHabitCompletion(
                id = habit.id,
                isCompleted = nextCompleted,
                streak = newStreak,
                date = if (nextCompleted) todayDate else habit.lastCompletedDate
            )
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    // --- Custom Profile Preferences Update ---
    fun saveProfile(name: String, bio: String, emoji: String, isDark: Boolean, lockPinCode: String) {
        userProfileName.value = name
        userProfileBio.value = bio
        userProfileEmoji.value = emoji
        appThemeDark.value = isDark
        appLockPin.value = lockPinCode
        if (lockPinCode.isNotEmpty()) {
            appLockUnlocked.value = false
        }
    }

    // --- Gemini AI Features (summarization, rewrite assistants, analyses) ---
    fun clearAiState() {
        _aiState.value = AiState.Idle
    }

    fun summarizeAndGenerateTasksFromNote(note: Note) {
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            try {
                val result = GeminiClient.summarizeAndExtractTasks(note.title, note.content)
                if (result != null) {
                    _aiState.value = AiState.Success(
                        summary = result.summary,
                        tasks = result.tasks,
                        noteTitle = note.title
                    )
                } else {
                    _aiState.value = AiState.Error("Gemini API key is not configured. Configure GEMINI_API_KEY inside the Secrets panel.")
                }
            } catch (e: Exception) {
                _aiState.value = AiState.Error("AI Connection Failed: ${e.localizedMessage}")
            }
        }
    }

    fun addAiGeneratedTasks(tasks: List<String>, tag: String) {
        viewModelScope.launch {
            tasks.forEach { taskTitle ->
                repository.insertTask(
                    Task(
                        title = taskTitle,
                        noteText = "Generated via AI note translation",
                        priority = "Medium",
                        tag = tag,
                        category = "Personal"
                    )
                )
            }
        }
    }

    fun triggerWritingAssistant(originalText: String, taskOption: String) {
        // taskOption: "Grammar Correction", "Expand Notes", "Rewrite Professional", "Synthesize Summary"
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _aiState.value = AiState.Error("Gemini key is missing. Add GEMINI_API_KEY in the AI Studio Secrets panel.")
                    return@launch
                }

                val prompt = """
                    You are a writing assistant in a premium notes app.
                    Please execute this action: "$taskOption".
                    Provide the edited version directly. Do not include markdown code blocks, conversational introductions, or commentary.
                    
                    Text:
                    $originalText
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt)))),
                    systemInstruction = ContentPart(parts = listOf(TextPart(text = "You are a professional editor. Output only the modified text.")))
                )

                val response = GeminiClient.apiService.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                _aiState.value = AiState.WritingAssistantSuccess(originalText, responseText.trim())
            } catch (e: Exception) {
                _aiState.value = AiState.Error("Writing edit failed: ${e.localizedMessage}")
            }
        }
    }

    fun triggerAiDailyPlanner(taskHeaders: List<String>) {
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _aiState.value = AiState.Error("Configure Gemini key to use AI Daily planner.")
                    return@launch
                }

                val prompt = """
                    Based on these active scheduled tasks, draft a beautiful, optimized daily planner routine schedule.
                    Sort them intelligently into Morning, Afternoon and Evening slots. Keep it highly concise and actionable.
                    
                    Tasks:
                    ${taskHeaders.joinToString("\n")}
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt)))),
                    systemInstruction = ContentPart(parts = listOf(TextPart(text = "You are a daily scheduler expert. Output formatted text.")))
                )

                val response = GeminiClient.apiService.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                _aiState.value = AiState.PlannerSuccess(responseText.trim())
            } catch (e: Exception) {
                _aiState.value = AiState.Error("Planning failed: ${e.localizedMessage}")
            }
        }
    }

    fun runProductivityAnalysis(allTasks: List<Task>, completedTasksCount: Int) {
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _aiState.value = AiState.Success(
                        summary = "Your default offline productivity is looking solid!",
                        tasks = emptyList(),
                        noteTitle = "Offline Metrics"
                    )
                    return@launch
                }

                val prompt = """
                    Analyze this user's current task completion status (Completed: $completedTasksCount, Total: ${allTasks.size}).
                    Task list titles with priority metrics:
                    ${allTasks.joinToString("\n") { "${it.title} - Priority: ${it.priority} - Category: ${it.category} - Status: ${it.status}" }}
                    
                    Return a concise review of:
                    1. Completion rates & estimated most productive hours.
                    2. Insights about categories.
                    3. Actionable improvements.
                    
                    Respond in this exact XML/JSON wrapper format or simple string.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt)))),
                    systemInstruction = ContentPart(parts = listOf(TextPart(text = "You are a data-driven personal coach.")))
                )

                val response = GeminiClient.apiService.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No insight report returned."
                
                val score = if (allTasks.isEmpty()) 100 else (completedTasksCount * 100) / allTasks.size
                _aiState.value = AiState.ProductivityAnalysis(score, responseText.trim())
            } catch (e: Exception) {
                _aiState.value = AiState.Error("Coaching failed: ${e.localizedMessage}")
            }
        }
    }
}
