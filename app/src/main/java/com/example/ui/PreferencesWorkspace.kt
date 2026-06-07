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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // Constants for counts
    val tasksCompleted = tasks.count { it.isCompleted }
    val tasksPending = tasks.count { !it.isCompleted }
    val notesCreated = notes.size
    val currentStreak = 5 // Premium default
    val focusHours = "18.5" // Simulated Focus mode duration
    val productivityScore = if (tasks.isEmpty()) 75 else (tasksCompleted * 100 / tasks.size).coerceAtLeast(40).coerceAtMost(100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Upper Profile Section with Avatar & Glassmorphism Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    1.dp, 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), 
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Emoji Avatar with Animated Indicator Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { isEditingProfile = !isEditingProfile },
                    contentAlignment = Alignment.Center
                ) {
                    Text(pEmoji, fontSize = 52.sp)
                }

                Spacer(Modifier.height(16.dp))

                if (!isEditingProfile) {
                    Text(
                        text = pName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = pBio,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { isEditingProfile = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("edit_profile_action")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Workspace Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Emoji selections
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            listOf("🚀", "💻", "🎨", "🧠", "🍀", "🔥", "✨").forEach { emo ->
                                val active = emo == editingEmoji
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { editingEmoji = emo }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emo, fontSize = 20.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = editingBio,
                            onValueChange = { editingBio = it },
                            label = { Text("Personal Motto / Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isEditingProfile = false }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    viewModel.saveProfile(editingName, editingBio, editingEmoji, isDarkTheme, lockPin)
                                    isEditingProfile = false
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Profile")
                            }
                        }
                    }
                }
            }
        }

        // Google Account Session Card
        val activeEmail by viewModel.activeUserEmail.collectAsStateWithLifecycle()
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeEmail == "local") "Local Offline Workspace" else "Google Account Session",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (activeEmail == "local") "Sign in to isolate dynamic cloud tasks" else activeEmail,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                TextButton(
                    onClick = {
                        viewModel.signOutCurrentAccount()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (activeEmail == "local") Icons.Default.KeyboardArrowUp else Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activeEmail == "local") "Link" else "Sign Out",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Stats Counter Section
        Text(
            text = "📊 MIND FLOW ANALYTICS STATUS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Column Stats
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stat Card 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tasks Done", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$tasksCompleted",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("$tasksPending items remaining", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Stat Card 2
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notes Formed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$notesCreated",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Right Column Stats
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stat Card 3
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Streak Level", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$currentStreak 🔥",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text("Active daily count", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Stat Card 4
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Focus Hours", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = focusHours,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF9F1C)
                        )
                        Text("Productivity: $productivityScore%", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Settings Cards Groups - Custom Glassmorphism Accent borders
        Text(
            text = "⚙ WORKSPACE SETTINGS & SECURITY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Glassmorphism System Settings Card
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Mode Ambience", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Optimize screen glare & battery drain", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.saveProfile(pName, pBio, pEmoji, it, lockPin) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Vault Biometric PIN settings
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔒 Vault Privacy Pin Door Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Secure system notes, records, sketches behind 4-digit lockout PIN standard.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draftPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    draftPin = it
                                }
                            },
                            placeholder = { Text("Enter 4-digit lock code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.saveProfile(pName, pBio, pEmoji, isDarkTheme, draftPin)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Secure")
                        }
                    }

                    if (lockPin.isNotEmpty()) {
                        Text(
                            text = "✓ Vault PIN activated. Safe and protected.",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "⚠ Vault PIN inactive. notes are currently unprotected.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Alarm periodicity sync rate
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔔 Background Notification Check Interval", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Automated background check interval for reminders & overdues.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                    Slider(
                        value = notifyMin.toFloat(),
                        onValueChange = { viewModel.notifyIntervalMinutes.value = it.toInt() },
                        valueRange = 10f..120f,
                        steps = 10
                    )

                    Text(
                        text = "Synchronize rate checks: every $notifyMin minutes.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Local Snapshot Sandbox Database state indicators
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("📁 Encrypted Sandbox Storage Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("- Tasks Registry database records: ${tasks.size} records", fontSize = 11.sp)
                Text("- Notes Registry database records: ${notes.size} records", fontSize = 11.sp)
                Text("Room SQLite Sandboxed database schema: V2 encryption level AES-256 enabled.", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cloud Backup Snapshot Archive", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
