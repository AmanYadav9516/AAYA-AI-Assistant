package com.aaya.assistant.engine.camera

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.ui.theme.NeonCyan
import com.aaya.assistant.ui.theme.VoidBlack
import java.io.OutputStream

class VoiceCameraActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceCameraCountdown(
                onCountdownFinished = {
                    captureAndSavePhoto()
                }
            )
        }
    }

    private fun captureAndSavePhoto() {
        val app = application as? AayaApplication
        app?.ttsManager?.speak("Smile! Capturing now.")

        try {
            // Launch device camera for hands-free capture
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(packageManager) != null) {
                startActivity(cameraIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            finish()
        }
    }

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, VoiceCameraActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun VoiceCameraCountdown(onCountdownFinished: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        val timer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsLeft = (millisUntilFinished / 1000).toInt() + 1
            }

            override fun onFinish() {
                secondsLeft = 0
                onCountdownFinished()
            }
        }
        timer.start()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (secondsLeft > 0) "$secondsLeft" else "Smile! 📸",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Hands-free Voice Camera: 3 Second Timer",
                color = Color.LightGray,
                fontSize = 16.sp
            )
        }
    }
}
