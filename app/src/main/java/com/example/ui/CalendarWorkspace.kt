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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.Task
import com.example.data.Habit
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarWorkspace(
    viewModel: MainViewModel,
    tasks: List<Task>,
    habits: List<Habit>
) {
    var selectedCalendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    var displayedMonthDate by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }

    // Navigation lists
    val calendarDays = remember(displayedMonthDate) {
        val daysList = ArrayList<Calendar?>()
        val tempCal = displayedMonthDate.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
        val blankDaysBefore = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
        
        for (i in 0 until blankDaysBefore) {
            daysList.add(null)
        }
        
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDays) {
            val cellDate = tempCal.clone() as Calendar
            cellDate.set(Calendar.DAY_OF_MONTH, i)
            daysList.add(cellDate)
        }
        daysList
    }

    val selectedTasks = tasks.filter { task ->
        task.dueDateLong != 0L && isSameDay(task.dueDateLong, selectedCalendarDate.timeInMillis)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month and Year Switcher Head
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedMonthDate.time),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Consolidated calendar schedules & timeline",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        val prev = displayedMonthDate.clone() as Calendar
                        prev.add(Calendar.MONTH, -1)
                        displayedMonthDate = prev
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev Month")
                }
                IconButton(
                    onClick = {
                        val next = displayedMonthDate.clone() as Calendar
                        next.add(Calendar.MONTH, 1)
                        displayedMonthDate = next
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }
        }

        // Calendar Grid Header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Monthly Date Cell Grid
        VerticalGridForCalendar(
            items = calendarDays,
            modifier = Modifier.fillMaxWidth()
        ) { cal ->
            if (cal == null) {
                Box(modifier = Modifier.aspectRatio(1f))
            } else {
                val isSelectedDay = isSameDay(cal.timeInMillis, selectedCalendarDate.timeInMillis)
                val isCurrentToday = isSameDay(cal.timeInMillis, System.currentTimeMillis())
                
                val dayTasks = tasks.filter { task ->
                    task.dueDateLong != 0L && isSameDay(task.dueDateLong, cal.timeInMillis)
                }
                val hasPendingTasks = dayTasks.any { !it.isCompleted }
                val hasCompletedTasks = dayTasks.isNotEmpty() && dayTasks.all { it.isCompleted }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(
                            if (isSelectedDay) MaterialTheme.colorScheme.primary
                            else if (isCurrentToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { selectedCalendarDate = cal }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                            fontWeight = if (isSelectedDay || isCurrentToday) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelectedDay) Color.White
                            else if (isCurrentToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Event Dot Indicators
                        if (dayTasks.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelectedDay) Color.White
                                            else if (hasCompletedTasks) Color(0xFF10B981)
                                            else Color(0xFFEF4444)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        // Selected Date Details & Dynamic Timeline list
        Text(
            text = "📅 Agenda: ${SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(selectedCalendarDate.time)}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        if (selectedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(36.dp)
                    )
                    Text("No Scheduled Milestones", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("There are no registered deadlines, tasks, or event intervals set for this date.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(selectedTasks) { task ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp, 40.dp)
                                    .clip(RoundedCornerShape(3.dp))
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
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${task.category} • Due: ${task.dueTime}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (task.isCompleted) Color(0xFF10B981).copy(alpha = 0.2f)
                                                else Color.LightGray.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (task.isCompleted) "Completed" else "Pending",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (task.isCompleted) Color(0xFF10B981) else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Minimalist Grid layout implementation for dates cells
@Composable
fun VerticalGridForCalendar(
    items: List<Calendar?>,
    modifier: Modifier = Modifier,
    content: @Composable (Calendar?) -> Unit
) {
    Column(modifier = modifier) {
        val chunked = items.chunked(7)
        chunked.forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        content(item)
                    }
                }
                // Fill up remaining elements in row if less than 7 units
                if (rowItems.size < 7) {
                    for (i in 0 until (7 - rowItems.size)) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

fun isSameDay(timeMillis1: Long, timeMillis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timeMillis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timeMillis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
