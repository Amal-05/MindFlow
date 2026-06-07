package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var renameLabel by remember { mutableStateOf("") }
    var supportText by remember { mutableStateOf("") }
    var recoveryMode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Infinity Workspace",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Advanced To-Dos, Sketches, AI Flashcards & Vaults",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when {
                            recoveryMode -> "Recover Secret Vault Keys"
                            isSignUp -> "Create Secure Cloud Profile"
                            else -> "Access Profile Management"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    if (supportText.isNotEmpty()) {
                        Text(
                            text = supportText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        )
                    }

                    if (!recoveryMode) {
                        if (isSignUp) {
                            OutlinedTextField(
                                value = renameLabel,
                                onValueChange = { renameLabel = it },
                                label = { Text("Workspace User Name") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Identity ID") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Secured Passphrase") },
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Enter account email") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Button(
                            onClick = {
                                if (recoveryMode) {
                                    if (email.isEmpty()) {
                                        supportText = "Please input email to verify account."
                                        return@Button
                                    }
                                    loading = true
                                    scope.launch {
                                        delay(1500)
                                        loading = false
                                        supportText = "✉️ Verification email sent! Reset key confirmed."
                                        recoveryMode = false
                                    }
                                } else {
                                    if (email.isEmpty() || password.isEmpty()) {
                                        supportText = "Both credentials require validation!"
                                        return@Button
                                    }
                                    loading = true
                                    scope.launch {
                                        delay(1200)
                                        if (isSignUp && renameLabel.isNotEmpty()) {
                                            viewModel.userProfileName.value = renameLabel
                                        }
                                        loading = false
                                        onAuthSuccess()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when {
                                    recoveryMode -> "Submit Recovery Request"
                                    isSignUp -> "Complete Cloud Sign-Up"
                                    else -> "Unlock Dashboard"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!recoveryMode && !isSignUp) {
                            // Google Authentication alternative
                            OutlinedButton(
                                onClick = {
                                    loading = true
                                    scope.launch {
                                        delay(1000)
                                        viewModel.userProfileName.value = "Google Sync Account"
                                        viewModel.userProfileEmoji.value = "🌐"
                                        loading = false
                                        onAuthSuccess()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Express Google Sign-In")
                            }
                        }
                    }

                    // Alternatives
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!recoveryMode) {
                            TextButton(onClick = { isSignUp = !isSignUp }) {
                                Text(
                                    text = if (isSignUp) "Have identity? Access login" else "Register biometric cloud",
                                    fontSize = 11.sp
                                )
                            }
                            TextButton(onClick = { recoveryMode = true }) {
                                Text("Forgot security key?", fontSize = 11.sp)
                            }
                        } else {
                            TextButton(onClick = { recoveryMode = false }) {
                                Text("Back to Authentication gateway", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
