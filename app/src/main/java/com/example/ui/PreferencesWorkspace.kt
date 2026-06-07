package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.Note
import com.example.data.Task

@Composable
fun PreferencesWorkspace(
    viewModel: MainViewModel,
    tasks: List<Task>,
    notes: List<Note>
) {
    val isDarkTheme by viewModel.appThemeDark.collectAsStateWithLifecycle()
    val lockPin by viewModel.appLockPin.collectAsStateWithLifecycle()
    val pName by viewModel.userProfileName.collectAsStateWithLifecycle()
    val pBio by viewModel.userProfileBio.collectAsStateWithLifecycle()
    val pEmoji by viewModel.userProfileEmoji.collectAsStateWithLifecycle()
    val notifyMin by viewModel.notifyIntervalMinutes.collectAsStateWithLifecycle()

    var editingName by remember { mutableStateOf(pName) }
    var editingBio by remember { mutableStateOf(pBio) }
    var editingEmoji by remember { mutableStateOf(pEmoji) }
    var isEditingProfile by remember { mutableStateOf(false) }

    var draftPin by remember { mutableStateOf(lockPin) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Preferences & Security Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text("Manage credential vault vaults, system theme properties and metadata configurations.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

        // 1. Theme Configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🎨 System Theme Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Atmosphere Theme", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Optimizes eye fatigue under dim ambient lighting rooms.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.appThemeDark.value = it }
                    )
                }
            }
        }

        // 2. Security Lock PIN settings
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🔒 Biometric Vault PIN Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Require a 4-digit numeric code before revealing Notebook database contents.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                draftPin = it
                            }
                        },
                        label = { Text("4-Digit Vault PIN") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.appLockPin.value = draftPin
                            viewModel.appLockUnlocked.value = draftPin.isEmpty()
                        }
                    ) {
                        Text("Secure")
                    }
                }

                if (lockPin.isNotEmpty()) {
                    Text(
                        text = "✓ Core Vault PIN status active: PIN requested upon launching app.",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "⚠ Vault PIN inactive. notes and checklists are exposed without authentication.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 3. User workspace profile metadata
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🚀 Workspace Profile Identification", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                if (!isEditingProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pEmoji, fontSize = 24.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(pName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(pBio, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        IconButton(onClick = { isEditingProfile = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("🚀", "💻", "🎨", "🌟", "🔥", "🧠").forEach { emo ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (editingEmoji == emo) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { editingEmoji = emo }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emo, fontSize = 18.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editingBio,
                            onValueChange = { editingBio = it },
                            label = { Text("Personal Motto / Status") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isEditingProfile = false }) { Text("Cancel") }
                            Button(onClick = {
                                viewModel.userProfileName.value = editingName
                                viewModel.userProfileBio.value = editingBio
                                viewModel.userProfileEmoji.value = editingEmoji
                                isEditingProfile = false
                            }) {
                                Text("Save Profile")
                            }
                        }
                    }
                }
            }
        }

        // 4. Notification Intervals & Sync Status
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🔔 Reminder Sync Engine Intervals", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Alarm sync tasks are executed offline in periodic scheduling queues.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                Slider(
                    value = notifyMin.toFloat(),
                    onValueChange = { viewModel.notifyIntervalMinutes.value = it.toInt() },
                    valueRange = 10f..120f,
                    steps = 10
                )

                Text(
                    text = "Sync checks: every $notifyMin minutes.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 5. Offline backup database parameters details
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📂 Local Sandbox Repository Status", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Total Encrypted Tasks database entries: ${tasks.size} records", fontSize = 11.sp)
                Text("Total Encrypted Notes database entries: ${notes.size} records", fontSize = 11.sp)
                Text("Room Database File Location: SQLite Sandboxed Folder Client Memory", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Sync Cloud")
                    Spacer(Modifier.width(8.dp))
                    Text("Backup Vault Memory Snapshot")
                }
            }
        }
    }
}
