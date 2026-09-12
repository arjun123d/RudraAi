package com.arjun.rudra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.arjun.rudra.ai.ApiKeyProvider
import com.arjun.rudra.command.CommandRouter
import com.arjun.rudra.command.RouterOutcome
import com.arjun.rudra.manager.PermissionManager
import com.arjun.rudra.voice.SpeechManager
import com.arjun.rudra.voice.TTSManager
import kotlinx.coroutines.launch

enum class RudraState { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

class MainActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager
    private lateinit var ttsManager: TTSManager
    private lateinit var router: CommandRouter

    private var state by mutableStateOf(RudraState.IDLE)
    private var transcript by mutableStateOf("")
    private var reply by mutableStateOf("Hey Rudra bolo, ba niche button e chepo.")
    private var pendingConfirm by mutableStateOf<(suspend () -> RouterOutcome)?>(null)
    private var showSettings by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        router = CommandRouter(this)
        ttsManager = TTSManager(
            this,
            onSpeakStart = { state = RudraState.SPEAKING },
            onSpeakDone = { state = RudraState.IDLE }
        )
        speechManager = SpeechManager(
            this,
            onResult = { text -> onUtterance(text) },
            onError = { err -> reply = err; state = RudraState.ERROR },
            onPartial = { partial -> transcript = partial }
        )

        val missing = PermissionManager.missing(this)
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())

        setContent { RudraScreen() }
    }

    private fun onUtterance(text: String) {
        transcript = text
        state = RudraState.THINKING
        lifecycleScope.launch {
            val outcome = router.handle(text)
            applyOutcome(outcome)
        }
    }

    private fun applyOutcome(outcome: RouterOutcome) {
        when (outcome) {
            is RouterOutcome.Speak -> {
                reply = outcome.text
                ttsManager.speak(outcome.text)
            }
            is RouterOutcome.NeedsConfirmation -> {
                reply = outcome.prompt
                pendingConfirm = outcome.onConfirm
                ttsManager.speak(outcome.prompt)
            }
            is RouterOutcome.NeedsPermission -> {
                reply = outcome.explanation
                ttsManager.speak(outcome.explanation)
                permissionLauncher.launch(arrayOf(outcome.permission))
            }
            is RouterOutcome.NeedsDisambiguation -> {
                reply = outcome.prompt
                ttsManager.speak(outcome.prompt)
            }
        }
    }

    private fun confirmPending() {
        val action = pendingConfirm ?: return
        pendingConfirm = null
        state = RudraState.THINKING
        lifecycleScope.launch { applyOutcome(action()) }
    }

    private fun startListening() {
        state = RudraState.LISTENING
        transcript = ""
        speechManager.startListening()
    }

    override fun onDestroy() {
        speechManager.destroy()
        ttsManager.shutdown()
        super.onDestroy()
    }

    @Composable
    private fun RudraScreen() {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF5B8CFF))) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0A0E17)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showSettings = true }) {
                                Text("⚙ Settings", color = Color(0xFF8A93A8))
                            }
                        }
                        Text(
                            "R U D R A",
                            color = Color(0xFF5B8CFF),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp
                        )
                        Text(
                            stateLabel(),
                            color = Color(0xFF8A93A8),
                            fontSize = 12.sp,
                            letterSpacing = 3.sp
                        )
                    }

                    Orb(state)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (transcript.isNotBlank()) {
                            Text(transcript, color = Color.White, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            reply,
                            color = Color(0xFFB8C0D4),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(Modifier.height(20.dp))

                        if (pendingConfirm != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { confirmPending() }) { Text("Yes, confirm") }
                                OutlinedButton(onClick = { pendingConfirm = null; reply = "Achha, bad diye dilam." }) {
                                    Text("Cancel")
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }

                        Button(
                            onClick = { startListening() },
                            enabled = state != RudraState.LISTENING,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Text(if (state == RudraState.LISTENING) "..." else "🎤")
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }

                if (showSettings) {
                    SettingsDialog(onDismiss = { showSettings = false })
                }
            }
        }
    }

    @Composable
    private fun SettingsDialog(onDismiss: () -> Unit) {
        var keyInput by remember { mutableStateOf(ApiKeyProvider.get(this) ?: "") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("AI API Key") },
            text = {
                Column {
                    Text("OpenAI (ba compatible) API key ta boshao — eta sudhu casual chat / free-form command er jonno lagbe.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("API Key") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    ApiKeyProvider.set(this, keyInput.trim())
                    reply = "API key save hoye geche."
                    onDismiss()
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }

    private fun stateLabel(): String = when (state) {
        RudraState.IDLE -> "SYSTEM ONLINE"
        RudraState.LISTENING -> "LISTENING..."
        RudraState.THINKING -> "THINKING..."
        RudraState.SPEAKING -> "SPEAKING..."
        RudraState.ERROR -> "ERROR"
    }

    @Composable
    private fun Orb(state: RudraState) {
        val infiniteTransition = rememberInfiniteTransition(label = "orb")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = if (state == RudraState.IDLE) 1.0f else 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        val color = when (state) {
            RudraState.LISTENING -> Color(0xFF5BFFB0)
            RudraState.THINKING -> Color(0xFFFFC85B)
            RudraState.SPEAKING -> Color(0xFF5B8CFF)
            RudraState.ERROR -> Color(0xFFFF5B5B)
            RudraState.IDLE -> Color(0xFF3A4A6B)
        }
        Box(
            modifier = Modifier
                .size((160 * pulse).dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(color, Color(0xFF0A0E17)))),
            contentAlignment = Alignment.Center
        ) {}
    }
}
