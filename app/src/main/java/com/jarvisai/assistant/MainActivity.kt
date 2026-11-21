package com.jarvisai.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private val Bg = Color(0xFF050B14)
private val Panel = Color(0xFF0B1726)
private val Cyan = Color(0xFF43E8FF)
private val Muted = Color(0xFF8BA7B8)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var onSpeechText: ((String) -> Unit)? = null
    private var onSpeechStatus: ((String) -> Unit)? = null

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else onSpeechStatus?.invoke("Microphone permission is needed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { onSpeechStatus?.invoke("Listening…") }
                override fun onBeginningOfSpeech() { onSpeechStatus?.invoke("Hearing you…") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { onSpeechStatus?.invoke("Processing…") }
                override fun onError(error: Int) { onSpeechStatus?.invoke("Didn't catch that. Tap the mic to try again.") }
                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!words.isNullOrBlank()) onSpeechText?.invoke(words)
                    onSpeechStatus?.invoke("Ready")
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        setContent {
            var recognizedSpeech by remember { mutableStateOf("") }
            var speechStatus by remember { mutableStateOf("SYSTEM ONLINE  •  READY") }
            onSpeechText = { recognizedSpeech = it }
            onSpeechStatus = { speechStatus = it }
            JarvisScreen(
                listen = { requestMicAndListen() },
                speak = { say(it) },
                recognizedSpeech = recognizedSpeech,
                speechStatus = speechStatus
            )
        }
    }

    private fun requestMicAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to VOXI")
        }
        speechRecognizer?.startListening(intent)
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun JarvisScreen(
    listen: () -> Unit,
    speak: (String) -> Unit,
    recognizedSpeech: String,
    speechStatus: String
) {
    var input by remember { mutableStateOf("") }
    val status = speechStatus
    val messages = remember { mutableStateListOf("VOXI: Hello. I am ready to assist you.") }

    LaunchedEffect(recognizedSpeech) {
        if (recognizedSpeech.isNotBlank()) input = recognizedSpeech
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add("YOU: $text")
        messages.add("VOXI: The AI connection is not configured yet. This starter build can display your messages and speak demo responses.")
        speak("The AI connection is not configured yet. I received: $text")
        input = ""
    }

    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF081827), Bg, Color(0xFF02050A)))
        ).padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.voxi_logo),
                contentDescription = "VOXI Voice Assistant logo",
                modifier = Modifier.size(76.dp),
                contentScale = ContentScale.Fit
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("VOXI AI", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("YOUR VOICE ASSISTANT", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Cyan, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(26.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(210.dp).border(1.dp, Cyan.copy(alpha = .35f), CircleShape)
                    .padding(12.dp).border(1.dp, Cyan.copy(alpha = .55f), CircleShape)
                    .padding(12.dp).background(
                        Brush.radialGradient(listOf(Color(0xFF15516A), Color(0xFF0B2034), Color(0xFF07111D))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AI", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light, letterSpacing = 8.sp)
                    Text("CORE", color = Cyan, fontSize = 11.sp, letterSpacing = 4.sp)
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        Text(status, color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction("VOICE", "Speak", Modifier.weight(1f)) { listen() }
            QuickAction("TOOLS", "Phone tools", Modifier.weight(1f)) { status = "PHONE TOOLS: COMING NEXT" }
            QuickAction("HISTORY", "Chats", Modifier.weight(1f)) { status = "CHAT HISTORY" }
        }
        Spacer(Modifier.height(18.dp))
        Text("CONVERSATION", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Panel.copy(alpha = .88f)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                Text(msg, color = if (msg.startsWith("YOU:")) Color.White else Cyan, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF102337)).border(1.dp, Cyan.copy(alpha = .35f), RoundedCornerShape(30.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("Ask VOXI anything…", color = Muted, fontSize = 14.sp)
                    inner()
                }
            )
            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Cyan,
                modifier = Modifier.padding(horizontal = 8.dp).size(22.dp).clickable { listen() })
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Cyan,
                modifier = Modifier.size(22.dp).clickable { sendMessage(input) })
        }
        Spacer(Modifier.height(8.dp))
        Text("DEMO BUILD • AI SERVER NOT CONNECTED", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun QuickAction(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Panel)
            .border(1.dp, Cyan.copy(alpha = .22f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 13.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, fontSize = 9.sp)
    }
}
