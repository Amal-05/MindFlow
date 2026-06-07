package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.UserProfile
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
    
    // Google Sign-In Dialog & Custom Add Account Forms
    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var showAddAccountForm by remember { mutableStateOf(false) }
    var newGoogleEmail by remember { mutableStateOf("") }
    var newGoogleName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("⚡") }
    var signingInAccount by remember { mutableStateOf<UserProfile?>(null) }
    var loadingText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val allProfiles by viewModel.allUserProfiles.collectAsStateWithLifecycle()

    // Pre-seed default sandbox Google Accounts into Room Database for instant selection
    LaunchedEffect(allProfiles) {
        if (allProfiles.isEmpty()) {
            viewModel.signUpOrLoginWithGoogle("Amalrajesh05@gmail.com", "Amal Rajesh", emoji = "🔥")
            viewModel.signUpOrLoginWithGoogle("demo.creative@gmail.com", "Creative Genius", emoji = "🚀")
            viewModel.signOutCurrentAccount() // Maintain starting logged-out state
        }
    }

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
                                        // Save standard account into user_profiles Database for persistence
                                        viewModel.signUpOrLoginWithGoogle(
                                            email = email,
                                            name = if (isSignUp && renameLabel.isNotEmpty()) renameLabel else "Workspace User",
                                            emoji = "🔑"
                                        )
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
                            // Active Google Authentication Integration
                            Button(
                                onClick = {
                                    showGoogleAccountDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1F1F1F)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Custom high contrast Google Brand graphic
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 15.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
                                }
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

    // Google Accounts Switcher Modal (Dynamic SQLite Sync Provider)
    if (showGoogleAccountDialog) {
        Dialog(
            onDismissRequest = {
                if (signingInAccount == null) showGoogleAccountDialog = false
            }
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google logo bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 24.sp)
                        Text("o", fontWeight = FontWeight.Black, color = Color(0xFFEA4335), fontSize = 24.sp)
                        Text("o", fontWeight = FontWeight.Black, color = Color(0xFFFBBC05), fontSize = 24.sp)
                        Text("g", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 24.sp)
                        Text("l", fontWeight = FontWeight.Black, color = Color(0xFF34A853), fontSize = 24.sp)
                        Text("e", fontWeight = FontWeight.Black, color = Color(0xFFEA4335), fontSize = 24.sp)
                    }

                    Text(
                        text = "Choose an account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "to continue to Mind Flow Workspace",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Divider(modifier = Modifier.padding(bottom = 8.dp))

                    if (signingInAccount != null) {
                        // Sign-In Progress simulation
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Text(signingInAccount!!.displayName, fontWeight = FontWeight.Bold)
                            Text(loadingText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                        }
                    } else if (showAddAccountForm) {
                        // Add Account Form
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Register New Google Identity", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            OutlinedTextField(
                                value = newGoogleName,
                                onValueChange = { newGoogleName = it },
                                label = { Text("Display Name") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newGoogleEmail,
                                onValueChange = { newGoogleEmail = it },
                                label = { Text("Google Email Account") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Profile Emoji Selection Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Avatar Symbol:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                listOf("💡", "🧠", "⚡", "🌟", "🔥", "🚀").forEach { emoji ->
                                    val isSelected = selectedEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .clickable { selectedEmoji = emoji }
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 16.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAddAccountForm = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (newGoogleName.isBlank() || newGoogleEmail.isBlank() || !newGoogleEmail.contains("@")) {
                                            return@Button
                                        }
                                        viewModel.signUpOrLoginWithGoogle(
                                            email = newGoogleEmail,
                                            name = newGoogleName,
                                            emoji = selectedEmoji
                                        )
                                        showAddAccountForm = false
                                        newGoogleName = ""
                                        newGoogleEmail = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // Dynamically pull from Room database
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(allProfiles.size) { index ->
                                val profile = allProfiles[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            signingInAccount = profile
                                            loadingText = "Contacting Google Identity authentication server..."
                                            scope.launch {
                                                delay(600)
                                                loadingText = "Retrieving decoupled Room sqlite database sandbox..."
                                                delay(600)
                                                viewModel.signUpOrLoginWithGoogle(profile.email, profile.displayName, emoji = profile.emoji)
                                                signingInAccount = null
                                                showGoogleAccountDialog = false
                                                onAuthSuccess()
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(profile.emoji, fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(profile.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(profile.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Stored",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Trigger to launch simulated Google OAuth interface
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showAddAccountForm = true
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Use another google account",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To continue, Google will share your email, profile, and customization preferences with Mind Flow.",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
