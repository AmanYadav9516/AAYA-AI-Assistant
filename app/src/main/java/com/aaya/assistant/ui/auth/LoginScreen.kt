package com.aaya.assistant.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.R
import com.aaya.assistant.data.remote.FirebaseUserManager
import com.aaya.assistant.ui.theme.*
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

    // Google Sign-In Client configuration
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
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
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
                                        Toast.makeText(context, "Welcome ${user.displayName ?: ""}!", Toast.LENGTH_SHORT).show()
                                        onContinue()
                                    }
                                } else {
                                    isLoading = false
                                    onContinue()
                                }
                            } else {
                                isLoading = false
                                errorMessage = authTask.exception?.localizedMessage ?: "Sign in failed"
                            }
                        }
                } else {
                    errorMessage = "Google token unavailable. You can continue as Guest."
                }
            } catch (e: Exception) {
                errorMessage = "Google Sign-In canceled or failed. You can continue as Guest."
            }
        }
    }

    // Soft 3D Claymorphic Palette (from user Image 2 inspiration)
    val clayLavenderBg = Color(0xFFF3EDF8)
    val clayCardBg = Color(0xFFFFFFFF)
    val clayPrimaryPill = Color(0xFF9C80D8)
    val clayAccentText = Color(0xFF5D478B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEFE6F7),
                        Color(0xFFF8F4FD),
                        Color(0xFFE9DCF5)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 3D Companion Clay Character Card Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Peeking character image from user upload
                Image(
                    painter = painterResource(id = R.drawable.companion_clay_character),
                    contentDescription = "AAYA AI Companion",
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .shadow(12.dp, RoundedCornerShape(32.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Claymorphic Card
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = clayCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFEDE4F7), RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title & Friendly Greeting
                    Text(
                        text = "✨ Welcome Back",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = clayAccentText
                    )
                    Text(
                        text = "Login to personalize your AI experience",
                        fontSize = 13.sp,
                        color = Color(0xFF8E83A0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
                    )

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFECEB),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFD32F2F),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Primary Action: Google 1-Tap Sign-In Button
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xFFFFFFFF),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(1.5.dp, Color(0xFFE0D4F5), RoundedCornerShape(26.dp))
                            .clickable(enabled = !isLoading) {
                                errorMessage = null
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = clayPrimaryPill,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Signing in...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = clayAccentText
                                )
                            } else {
                                // Google G Icon representation
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "G",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF4285F4)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Sign in with Google",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = clayAccentText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Divider: "or"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5DAF2))
                        Text(
                            text = "or",
                            fontSize = 12.sp,
                            color = Color(0xFFAAA0BA),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5DAF2))
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Secondary Action: Continue as Guest (Offline Matrix Bypass)
                    Button(
                        onClick = {
                            userManager.continueAsGuest()
                            Toast.makeText(context, "Continuing in Offline Guest Mode", Toast.LENGTH_SHORT).show()
                            onContinue()
                        },
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = clayPrimaryPill
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(6.dp, RoundedCornerShape(26.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🚀 Continue as Guest",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Privacy & $0 Guarantee note
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF88D49E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "100% Free • Zero Spam • On-Device Privacy",
                            fontSize = 11.sp,
                            color = Color(0xFF8C7FA3),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
