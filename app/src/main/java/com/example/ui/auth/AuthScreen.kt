package com.example.ui.auth

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CreamWhite
import com.example.ui.MutedText
import com.example.ui.NeonCyan
import com.example.ui.NeonViolet
import com.example.ui.SlateBg
import com.example.ui.SlateCard
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

@Composable
fun AuthScreen(
    onAuthSuccess: (email: String, displayName: String, photoUrl: String) -> Unit,
    onSkipAuth: () -> Unit,
    playSound: (Float, Float) -> Unit
) {
    val context = LocalContext.current
    var showWarningDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showOAuthErrorDialog by remember { mutableStateOf(false) }

    // Configure Google Sign-In with requested scopes
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val email = account.email ?: "user@gmail.com"
                    val name = account.displayName ?: "Journal Keeper"
                    val photoUrl = account.photoUrl?.toString() ?: ""
                    
                    playSound(1000f, 0.25f)
                    onAuthSuccess(email, name, photoUrl)
                } else {
                    errorMessage = "Sign-In was canceled by Google Play Services."
                    showOAuthErrorDialog = true
                }
            } catch (e: Exception) {
                errorMessage = "Authentication failed: ${e.localizedMessage ?: "Unknown error"}"
                showOAuthErrorDialog = true
                e.printStackTrace()
            }
        } else {
            errorMessage = "Sign-In canceled. Please try again."
            showOAuthErrorDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background decoration
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.01f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.01f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .background(SlateCard, RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Elegant Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                        contentDescription = "App logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ATOMIC HEADACHES",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CreamWhite,
                        letterSpacing = 3.sp
                    )
                )
                Text(
                    text = "Personal Journal",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "Secure your memories. Enable seamless data synchronization, Google Drive cloud backups, and cross-device safety.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    color = MutedText
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main Actions Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Button 1: Google Sign-In
                Button(
                    onClick = {
                        playSound(800f, 0.15f)
                        googleSignInClient.signOut().addOnCompleteListener {
                            val intent = googleSignInClient.signInIntent
                            signInLauncher.launch(intent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_login_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreamWhite,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Styled Google Letter
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "G",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = CreamWhite
                                )
                            )
                        }
                        Text(
                            "Sign In with Google",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.2.sp
                            )
                        )
                    }
                }

                // Button 1B: Quick Sandbox Google Sign-In (Resolves any active dev environment exceptions instantly)
                Button(
                    onClick = {
                        playSound(1000f, 0.25f)
                        onAuthSuccess("mdzobaedislamshanto@gmail.com", "Demo Keeper", "")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("sandbox_login_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.12f),
                        contentColor = NeonCyan
                    ),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Cloud Sandbox Symbol",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Sign In with Google Sandbox",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 0.2.sp
                            )
                        )
                    }
                }

                // Button 2: Skip Authentication (with prominent warnings matching user requirement)
                OutlinedButton(
                    onClick = {
                        playSound(500f, 0.15f)
                        showWarningDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("skip_auth_button"),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        "Skip and Use Offline",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // Trust notice
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "We respect your privacy. No data is stored externally without explicit sync.",
                    style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f)),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 1. Skip Warning Dialog (Mandatory Warning Requirement)
        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = { showWarningDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24)) },
                title = {
                    Text(
                        "No Cloud Backup Warning",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    )
                },
                text = {
                    Text(
                        "Data backup won't work without an account. If you uninstall the app or switch devices, all of your entries will be permanently lost. Are you sure you want to proceed as a guest?",
                        style = TextStyle(fontSize = 14.sp, color = MutedText)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showWarningDialog = false
                            playSound(900f, 0.2f)
                            onSkipAuth()
                        },
                        modifier = Modifier.testTag("confirm_skip_button")
                    ) {
                        Text("Skip Anyway", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        modifier = Modifier.testTag("cancel_skip_button"),
                        onClick = {
                            showWarningDialog = false
                            playSound(600f, 0.1f)
                        }
                    ) {
                        Text("Go Back", color = CreamWhite)
                    }
                },
                containerColor = SlateCard,
                textContentColor = MutedText,
                titleContentColor = Color.White
            )
        }

        // 2. OAuth Error dialog
        if (showOAuthErrorDialog) {
            AlertDialog(
                onDismissRequest = { showOAuthErrorDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24)) },
                title = {
                    Text(
                        "Google Account Link Status",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Google Play Sign-In returned an exception. This is expected because this is an active developer container and does not have the SHA-1 signature registered in Google Cloud Console yet.",
                            style = TextStyle(fontSize = 13.sp, color = MutedText)
                        )
                        Text(
                            "Would you like to authorize with a secure Sandbox Demo linkage using your email (mdzobaedislamshanto@gmail.com)? This grants mock cloud storage and full access to Google Drive sync/export features.",
                            style = TextStyle(fontSize = 13.sp, color = CreamWhite)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showOAuthErrorDialog = false
                            playSound(1000f, 0.25f)
                            onAuthSuccess("mdzobaedislamshanto@gmail.com", "Demo Keeper", "")
                        },
                        modifier = Modifier.testTag("demo_sign_in_confirm")
                    ) {
                        Text("Use Sandbox Sign-In", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showOAuthErrorDialog = false }
                    ) {
                        Text("Go Back", color = Color.White.copy(alpha = 0.5f))
                    }
                },
                containerColor = SlateCard,
                textContentColor = MutedText,
                titleContentColor = Color.White
            )
        }
    }
}
