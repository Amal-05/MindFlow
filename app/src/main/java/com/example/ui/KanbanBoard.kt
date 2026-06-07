package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.Task

@Composable
fun KanbanBoardScreen(
    viewModel: MainViewModel,
    tasks: List<Task>,
    onSelectTask: (Task) -> Unit,
    onAddTaskDirectly: (String) -> Unit
) {
    val statuses = listOf("Pending", "In Progress", "Review", "Completed")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Interactive Kanban Workflow",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Organize task statuses dynamically using rapid transfer elements.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = {
                    // Quick task template
                    onAddTaskDirectly("Pending")
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add quick board card")
            }
        }

        // Horizontal scrolling columns representation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            statuses.forEach { statusName ->
                KanbanCol(
                    status = statusName,
                    tasks = tasks.filter {
                        // Support standard matching or default completed status
                        if (statusName == "Completed") it.isCompleted || it.status == "Completed"
                        else if (statusName == "Pending") !it.isCompleted && (it.status == "Pending" || it.status.isEmpty())
                        else !it.isCompleted && it.status == statusName
                    },
                    onMoveStatus = { task, nextStatus ->
                        viewModel.updateTask(
                            task.copy(
                                status = nextStatus,
                                isCompleted = (nextStatus == "Completed")
                            )
                        )
                    },
                    onSelectTask = onSelectTask,
                    onAddNew = { onAddTaskDirectly(statusName) }
                )
            }
        }
    }
}

@Composable
fun KanbanCol(
    status: String,
    tasks: List<Task>,
    onMoveStatus: (Task, String) -> Unit,
    onSelectTask: (Task) -> Unit,
    onAddNew: () -> Unit
) {
    val statusColor = when (status) {
        "Pending" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        "In Progress" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        "Review" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
        "Completed" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val headerColor = when (status) {
        "Pending" -> MaterialTheme.colorScheme.error
        "In Progress" -> MaterialTheme.colorScheme.primary
        "Review" -> MaterialTheme.colorScheme.tertiary
        "Completed" -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Sticky Header for Status Designation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusColor)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(headerColor, CircleShape)
                )
                Text(
                    text = status,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tasks.size.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { onSelectTask(task) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (task.priority) {
                                            "Critical" -> Color.Red.copy(alpha = 0.1f)
                                            "High" -> Color.Magenta.copy(alpha = 0.1f)
                                            "Medium" -> Color.Blue.copy(alpha = 0.1f)
                                            else -> Color.Gray.copy(alpha = 0.1f)
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = task.priority,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (task.priority) {
                                        "Critical" -> Color.Red
                                        "High" -> Color(0xFFC026D3)
                                        "Medium" -> Color.Blue
                                        else -> Color.DarkGray
                                    }
                                )
                            }

                            if (task.tag.isNotEmpty()) {
                                Text(
                                    text = "#${task.tag}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (task.noteText.isNotEmpty()) {
                            Text(
                                text = task.noteText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )

                        // Arrows for Status Transfer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val nextStatus = when (status) {
                                        "In Progress" -> "Pending"
                                        "Review" -> "In Progress"
                                        "Completed" -> "Review"
                                        else -> "Pending"
                                    }
                                    if (nextStatus != status) {
                                        onMoveStatus(task, nextStatus)
                                    }
                                },
                                enabled = status != "Pending"
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Move Left",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = if (task.dueTime.isNotEmpty()) "⏰ ${task.dueTime}" else "No Deadline",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            IconButton(
                                onClick = {
                                    val nextStatus = when (status) {
                                        "Pending" -> "In Progress"
                                        "In Progress" -> "Review"
                                        "Review" -> "Completed"
                                        else -> "Completed"
                                    }
                                    if (nextStatus != status) {
                                        onMoveStatus(task, nextStatus)
                                    }
                                },
                                enabled = status != "Completed"
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Move Right",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onAddNew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Project Card", fontSize = 11.sp)
                }
            }
        }
    }
}
