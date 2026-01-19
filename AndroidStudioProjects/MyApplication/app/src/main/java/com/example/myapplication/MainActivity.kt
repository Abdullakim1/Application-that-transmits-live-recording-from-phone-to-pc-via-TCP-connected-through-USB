package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.net.ServerSocket

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MicrophoneScreen()
        }
    }
}

@Composable
fun MicrophoneScreen() {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Initializing Auto-Start...") }
    var isStreaming by remember { mutableStateOf(false) }

    fun startStreaming() {
        // Prevent duplicate threads if already running
        if (isStreaming) return

        Thread {
            try {
                // 1. Open a "Door" (Server) on Port 5000
                // We wrap this in try/catch specifically for "Address already in use" errors
                val serverSocket = try {
                    ServerSocket(5000)
                } catch (e: Exception) {
                    // If port is busy, it usually means the previous socket didn't close fast enough
                    // or the app rotated. We simply return to avoid crash.
                    return@Thread
                }

                // Update UI: Waiting for laptop...
                statusText = "Waiting for Laptop to connect..."

                // 2. Wait here until Laptop connects
                val clientSocket = serverSocket.accept()

                statusText = "🔴 LIVE! Streaming to Laptop..."
                isStreaming = true

                val outputStream = clientSocket.getOutputStream()
                val minBufferSize = android.media.AudioRecord.getMinBufferSize(
                    44100,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT
                )

                // 3. Setup Microphone
                val recorder = android.media.AudioRecord(
                    android.media.MediaRecorder.AudioSource.MIC,
                    44100,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize
                )

                recorder.startRecording()
                val buffer = ByteArray(minBufferSize)

                // 4. THE LOOP: Read Mic -> Send to Laptop
                while (isStreaming) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        try {
                            outputStream.write(buffer, 0, read)
                        } catch (e: Exception) {
                            // If laptop disconnects, stop the loop
                            isStreaming = false
                        }
                    }
                }

                // Cleanup
                recorder.stop()
                recorder.release()
                clientSocket.close()
                serverSocket.close()
                statusText = "Ready to connect again"

            } catch (e: Exception) {
                e.printStackTrace()
                statusText = "Error: ${e.message}"
                isStreaming = false
            }
        }.start()
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) startStreaming()
            else statusText = "Permission Denied. Cannot Stream."
        }
    )

    // --- AUTOMATIC START LOGIC ---
    // LaunchedEffect runs exactly once when the app screen loads
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startStreaming()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // UI
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = statusText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))

        // We keep the button just in case to stop it manually
        Button(onClick = {
            if (isStreaming) {
                isStreaming = false
                statusText = "Stopping..."
            } else {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                    startStreaming()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }) {
            Text(if (isStreaming) "Stop Stream" else "Restart Stream")
        }
    }
}
