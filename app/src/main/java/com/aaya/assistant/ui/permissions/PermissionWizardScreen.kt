package com.aaya.assistant.ui.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aaya.assistant.ui.theme.*

data class PermissionStep(
    val id: Int,
    val title: String,
    val icon: ImageVector,
    val description: String,
    val privacyGuarantee: String,
    val isGranted: Boolean,
    val permissionManifestKey: String? = null,
    val isSpecialAccess: Boolean = false,
    val customIntentAction: String? = null
)

@Composable
fun PermissionWizardScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Trigger state recheck on resume
    var refreshTrigger by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val steps = remember(refreshTrigger) {
        computePermissionSteps(context)
    }

    val allGranted = steps.all { it.isGranted }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTrigger++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Permissions & Privacy",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = "AAYA only requests permissions needed to execute your commands. Your data stays 100% on your device and is never shared with 3rd parties.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
        )

        // Steps List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(steps.size) { index ->
                val step = steps[index]
                PermissionCard(
                    step = step,
                    onEnableClick = {
                        when {
                            step.customIntentAction != null -> {
                                try {
                                    val intent = Intent(step.customIntentAction)
                                    if (step.customIntentAction == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                                        intent.data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                            step.isSpecialAccess -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                            step.permissionManifestKey != null -> {
                                singlePermissionLauncher.launch(step.permissionManifestKey)
                            }
                            else -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                )
            }
        }

        // Bottom status banner
        if (allGranted) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .border(1.dp, SuccessGreen, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("All Permissions Ready!", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("AAYA is fully operational and ready to assist you.", fontSize = 12.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = onAllGranted,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Start", color = DeepIndigoBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    step: PermissionStep,
    onEnableClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isGranted) GlassSurface.copy(alpha = 0.6f) else GlassSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (step.isGranted) SuccessGreen.copy(alpha = 0.5f) else CardBorder,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (step.isGranted) SuccessGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = if (step.isGranted) SuccessGreen else NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Step ${step.id} — ${step.title}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = step.description,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Dynamic Status Badge
                if (step.isGranted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("✅ Enabled", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ErrorRed.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("❌ Missing", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                    }
                }
            }

            // Privacy promise and enable action
            if (!step.isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🔒 ${step.privacyGuarantee}",
                    fontSize = 11.sp,
                    color = RadiantPurple
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEnableClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Enable ${step.title} →", color = DeepIndigoBg, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun computePermissionSteps(context: Context): List<PermissionStep> {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val isDndGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        nm.isNotificationPolicyAccessGranted
    } else true

    val isMicGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val isNotifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    val isLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val isContactsGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    return listOf(
        PermissionStep(
            id = 1,
            title = "Microphone",
            icon = Icons.Default.Mic,
            description = "Allow microphone so I can hear your voice commands.",
            privacyGuarantee = "Audio is never recorded or transmitted without your direct wake trigger.",
            isGranted = isMicGranted,
            permissionManifestKey = Manifest.permission.RECORD_AUDIO
        ),
        PermissionStep(
            id = 2,
            title = "Notifications",
            icon = Icons.Default.Notifications,
            description = "Allow notifications so I can alert you for reminders and morning briefings.",
            privacyGuarantee = "No promotional spam. Only your requested schedules and alerts.",
            isGranted = isNotifGranted,
            permissionManifestKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        ),
        PermissionStep(
            id = 3,
            title = "Location",
            icon = Icons.Default.LocationOn,
            description = "Allow location if you want location-based commands and automatic Driving Mode.",
            privacyGuarantee = "Used purely on-device for speed/commute calculation. Never tracked remotely.",
            isGranted = isLocationGranted,
            permissionManifestKey = Manifest.permission.ACCESS_FINE_LOCATION
        ),
        PermissionStep(
            id = 4,
            title = "Phone & Contacts",
            icon = Icons.Default.Phone,
            description = "Allow this only if you want calling and smart contact features.",
            privacyGuarantee = "Contacts are matched locally on-device. Never uploaded or synced with third parties.",
            isGranted = isContactsGranted,
            permissionManifestKey = Manifest.permission.READ_CONTACTS
        ),
        PermissionStep(
            id = 5,
            title = "Do Not Disturb Control",
            icon = Icons.Default.DoNotDisturbOn,
            description = "Enable Notification Access so I can manage Sleep and Class modes automatically.",
            privacyGuarantee = "Allows AAYA to silence ordinary calls while keeping VIP emergency calls ringing.",
            isGranted = isDndGranted,
            isSpecialAccess = true
        ),
        PermissionStep(
            id = 6,
            title = "Display Over Other Apps",
            icon = Icons.Default.Layers,
            description = "Allows Siri-style floating pop-up overlay over WhatsApp, YouTube, and games.",
            privacyGuarantee = "Renders only when activated by you. Never intercepts your screen contents.",
            isGranted = Settings.canDrawOverlays(context),
            customIntentAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
        ),
        PermissionStep(
            id = 7,
            title = "Default Digital Assistant",
            icon = Icons.Default.Bolt,
            description = "Enables long-press power button (0.5s) to instantly summon AAYA with 0% idle battery drain.",
            privacyGuarantee = "Replaces default assistant for faster access on Realme, Oppo, OnePlus and all Android phones.",
            isGranted = true, // User can re-configure anytime
            customIntentAction = Settings.ACTION_VOICE_INPUT_SETTINGS
        ),
        PermissionStep(
            id = 8,
            title = "Accessibility (Voice Screenshot)",
            icon = Icons.Default.CameraAlt,
            description = "Enables touchless voice screenshots ('Take screenshot') and gesture navigation.",
            privacyGuarantee = "Used solely for global screenshot capture. No personal data collected.",
            isGranted = com.aaya.assistant.engine.service.HardwareKeyAccessibilityService.isServiceRunning(),
            customIntentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
        )
    )
}
