package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AiState {
    object Idle : AiState
    object Loading : AiState
    data class Success(val summary: String, val tasks: List<String>, val noteTitle: String) : AiState
    data class Error(val message: String) : AiState
}

class MainViewModel(private val repository: TaskAndNoteRepository) : ViewModel() {

    val selectedTag = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")
    
    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setTag(tag: String) {
        selectedTag.value = tag
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    // --- Task CRUD operations ---
    fun addTask(title: String, description: String, priority: String, tag: String, dueDateLong: Long) {
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    title = title,
                    noteText = description,
                    priority = priority,
                    tag = tag,
                    dueDateLong = dueDateLong
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
            repository.updateTaskCompletion(task.id, !task.isCompleted)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // --- Note CRUD operations ---
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

    // --- Gemini AI Co-Pilot feature ---
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
                    _aiState.value = AiState.Error("Gemini API key is not configured or rate-limited. Ensure GEMINI_API_KEY is active in the Secrets Panel.")
                }
            } catch (e: Exception) {
                _aiState.value = AiState.Error("Failed to connect: ${e.localizedMessage}")
            }
        }
    }

    fun addAiGeneratedTasks(tasks: List<String>, tag: String) {
        viewModelScope.launch {
            tasks.forEach { taskTitle ->
                repository.insertTask(
                    Task(
                        title = taskTitle,
                        noteText = "Generated via AI summary from Note analysis",
                        priority = "Medium",
                        tag = tag
                    )
                )
            }
        }
    }
}