package com.aaya.assistant.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aaya.assistant.ui.theme.*

@Composable
fun UserAgreementDialog(
    onAccept: () -> Unit
) {
    val context = LocalContext.current

    // Cinematic Pulsing Glow Animation
    val infiniteTransition = rememberInfiniteTransition(label = "agreement_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = {}, // Cannot be dismissed without accepting
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, CardBorder, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cinematic Glowing Orb Icon
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.radialGradient(
                                    listOf(NeonCyan.copy(alpha = 0.35f), RadiantPurple.copy(alpha = 0.2f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Privacy Shield",
                            tint = NeonCyan,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pill Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = NeonCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "🛡️ 100% PRIVATE & TRANSPARENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Welcome to AAYA Assistant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Before you step into India's fastest voice companion, here is how we protect and honor your personal data:",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Guarantee items
                    PrivacyItemRow(
                        icon = Icons.Default.Lock,
                        iconTint = AccentMint,
                        title = "Zero Data Selling",
                        desc = "Your personal information, queries, and contacts are NEVER sold or leased to third-party ad networks."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PrivacyItemRow(
                        icon = Icons.Default.OfflinePin,
                        iconTint = NeonCyan,
                        title = "Local & Offline-First Core",
                        desc = "Speech recognition and contact matching stay strictly on your phone. No audio is ever recorded or kept on external servers."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PrivacyItemRow(
                        icon = Icons.Default.VpnKey,
                        iconTint = RadiantPurple,
                        title = "256-Bit Hardware Encryption",
                        desc = "API keys and sensitive tokens are encrypted inside Android Keystore hardware security."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PrivacyItemRow(
                        icon = Icons.Default.VerifiedUser,
                        iconTint = CoralAccent,
                        title = "You Are In Total Control",
                        desc = "You can wipe stored memories, notes, and local caches anytime with a single tap in Settings."
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Privacy policy link
                    Text(
                        text = "Read Full Privacy Policy ↗",
                        fontSize = 12.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://amanyadav9516.github.io/AAYA-AI-Assistant/privacy.html")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Agreement Button
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DeepIndigoBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I Agree & Get Started ✨",
                            color = DeepIndigoBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconTint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}
