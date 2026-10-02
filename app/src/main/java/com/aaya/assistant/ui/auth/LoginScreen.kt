package com.aaya.assistant.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.R
import com.aaya.assistant.data.remote.FirebaseUserManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    userManager: FirebaseUserManager,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Google Sign-In Client configuration with user's verified Web Client ID
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                isLoading = true
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = authTask.result?.user
                            if (user != null) {
                                scope.launch {
                                    userManager.syncUserOnLogin(user)
                                    isLoading = false
                                    Toast.makeText(context, "Welcome ${user.displayName ?: "Friend"}!", Toast.LENGTH_SHORT).show()
                                    onContinue()
                                }
                            } else {
                                isLoading = false
                                onContinue()
                            }
                        } else {
                            isLoading = false
                            errorMessage = authTask.exception?.localizedMessage ?: "Firebase authentication failed"
                        }
                    }
            } else {
                errorMessage = "Google Token unavailable. Tap 'Continue as Guest' to enter immediately."
            }
        } catch (e: ApiException) {
            errorMessage = when (e.statusCode) {
                12500 -> "Google Play Services sign-in canceled. You can continue as Guest."
                10 -> "Configuration check in progress. Tap 'Continue as Guest' to bypass."
                else -> "Sign-in status: ${e.statusCode}. You can continue as Guest."
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Sign-in encountered an issue. You can continue as Guest."
        }
    }

    // 3D Claymorphic Palette (from user's design reference)
    val clayBackgroundTop = Color(0xFFF1E9F8)
    val clayBackgroundMid = Color(0xFFF7F3FC)
    val clayBackgroundBot = Color(0xFFEDE3F6)
    val clayCardWhite = Color(0xFFFFFFFF)
    val clayPrimaryPurple = Color(0xFF9E84D7)
    val clayPurpleDark = Color(0xFF553D84)
    val claySubtitleText = Color(0xFF8679A1)
    val clayPillBorder = Color(0xFFE4D9F5)
    val clayHeartPink = Color(0xFFFF7E98)
    val clayGuestBg = Color(0xFFF4ECFA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(clayBackgroundTop, clayBackgroundMid, clayBackgroundBot)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 1. TOP CLAYMORPHIC WELCOME TABLET (Matching user's mockup)
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = clayCardWhite,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFF3ECFA), RoundedCornerShape(32.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    // Floating Clay Heart
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFEEF2),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = clayHeartPink,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "✨ Welcome Back ✨",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = clayPurpleDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Login to continue your journey with AAYA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = claySubtitleText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 3D COMPANION CLAY MASCOT AVATAR
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEADBFA), clayPrimaryPurple)
                        )
                    )
                    .border(3.dp, Color(0xFFFFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "💜",
                        fontSize = 42.sp
                    )
                    Text(
                        text = "AAYA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = clayPurpleDark,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. MAIN CLAYMORPHIC ACTION CARD
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = clayCardWhite,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFF3ECFA), RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Instant 1-Tap Access",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = clayPurpleDark
                    )
                    Text(
                        text = "No password needed • Zero friction setup",
                        fontSize = 12.sp,
                        color = claySubtitleText,
                        modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                    )

                    // Error Message Banner (if any)
                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFF0F0),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD2D2)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFD32F2F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // PRIMARY ACTION: Google 1-Tap Sign-In Button (Clay Pill)
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xFFFFFFFF),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(1.5.dp, clayPillBorder, RoundedCornerShape(26.dp))
                            .clickable(enabled = !isLoading) {
                                errorMessage = null
                                isLoading = true
                                try {
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Could not open Google sign in: ${e.localizedMessage}"
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = clayPrimaryPurple,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Connecting to Google...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = clayPurpleDark
                                )
                            } else {
                                // Google "G" Badge
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF7F8FA),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "G",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF4285F4)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sign in with Google",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = clayPurpleDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // DIVIDER
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFEDE4F7)
                        )
                        Text(
                            text = "  or continue with  ",
                            fontSize = 12.sp,
                            color = claySubtitleText,
                            fontWeight = FontWeight.Medium
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFEDE4F7)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECONDARY ACTION: Continue as Guest (Offline Bypass)
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = clayGuestBg,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.2.dp, Color(0xFFE9DCF5), RoundedCornerShape(26.dp))
                            .clickable {
                                userManager.continueAsGuest()
                                Toast.makeText(context, "Continuing in 100% Offline Guest Mode", Toast.LENGTH_SHORT).show()
                                onContinue()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Continue as Guest (100% Offline)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = clayPurpleDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = clayPrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Privacy Shield Footer Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF34A853),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "256-bit Secure Encryption • Zero Data Selling",
                            fontSize = 11.sp,
                            color = Color(0xFF7A6F8F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
