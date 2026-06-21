package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Custom Theme colors
val SlateBg = Color(0xFF040406)      // Jet black deep space canvas
val SlateCard = Color(0xFF0C0C12)    // Slate absolute dark gray card surface
val CreamWhite = Color(0xFFEADDC9)   // Warm parchment branding color
val NeonCyan = Color(0xFF22D3EE)     // Cyber blue orb aura
val NeonViolet = Color(0xFF8B5CF6)   // Mystic amethyst orb aura
val MutedText = Color(0xFF9CA3AF)    // High contrast scale text

private val LOCATIONS = listOf("Home", "Campus", "Outside", "Lab", "Transit")
private val MOODS = listOf("Calm", "Focused", "Tired", "Reflective", "Restless", "Clear", "Analytical", "Skeptical")
private val FILTERS = listOf("All", "Today", "This Week", "This Month", "This Year", "Lifetime", "Mood", "Analytics")

@Composable
fun AtomicHeadachesApp(viewModel: NoteViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
    ) {
        // Dynamic Glowing Background Orbs
        AmbientOrb(
            color = NeonCyan,
            size = 280.dp,
            alpha = 0.12f,
            offsetX = (-60).dp,
            offsetY = (-80).dp,
            delayMs = 0
        )
        AmbientOrb(
            color = NeonViolet,
            size = 240.dp,
            alpha = 0.10f,
            alignment = Alignment.TopEnd,
            offsetX = 60.dp,
            offsetY = 120.dp,
            delayMs = 1200
        )
        AmbientOrb(
            color = Color.White,
            size = 260.dp,
            alpha = 0.06f,
            alignment = Alignment.BottomCenter,
            offsetX = 0.dp,
            offsetY = 100.dp,
            delayMs = 2400
        )

        // Overlay textures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.01f)
                        )
                    )
                )
        )

        val showOnboarding by viewModel.showOnboarding
        val onboardingDone by viewModel.onboardingDone
        val composerOpen by viewModel.composerOpen
        val detailOpenId by viewModel.detailOpenId
        val analyticsOpen by viewModel.analyticsOpen
        val notesList by viewModel.notes.collectAsState()
        val authCompleted by viewModel.authCompleted
        val authSkipped by viewModel.authSkipped
        val bookReaderOpen by viewModel.bookReaderOpen

        // 1. Deciding what primary screen / panel container is visible
        if (!authCompleted && !authSkipped) {
            com.example.ui.auth.AuthScreen(
                onAuthSuccess = { email, displayName, photoUrl ->
                    viewModel.completeAuth(email, displayName, photoUrl)
                },
                onSkipAuth = {
                    viewModel.skipAuth()
                },
                playSound = { freq, dur -> viewModel.playSound(freq, dur) }
            )
        } else if (!onboardingDone && showOnboarding) {
            OnboardingOverlay(
                onFinish = { title, body -> viewModel.completeOnboardingWithNote(title, body) },
                playSound = { freq, dur -> viewModel.playSound(freq, dur) }
            )
        } else if (bookReaderOpen) {
            com.example.ui.reader.BookReader(
                notes = notesList,
                onClose = { viewModel.bookReaderOpen.value = false },
                playSound = { freq, dur -> viewModel.playSound(freq, dur) }
            )
        } else {
            // Main App stream
            MainStream(
                viewModel = viewModel,
                notesList = notesList
            )

            // Compose Overlay Sheets
            AnimatedVisibility(
                visible = composerOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                viewModel.draft.value?.let { activeDraft ->
                    ComposerSheet(
                        draft = activeDraft,
                        viewModel = viewModel,
                        onSave = { viewModel.saveDraft() },
                        onClose = {
                            viewModel.composerOpen.value = false
                            viewModel.draft.value = null
                            viewModel.editingId.value = null
                            viewModel.focusMode.value = false
                            viewModel.playSound(500f, 0.12f)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = detailOpenId != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                val activeNote = notesList.find { it.id == detailOpenId }
                if (activeNote != null) {
                    DetailSheet(
                        note = activeNote,
                        viewModel = viewModel,
                        onClose = { viewModel.detailOpenId.value = null },
                        onEdit = { viewModel.openEdit(activeNote) },
                        onDelete = { viewModel.deleteNote(activeNote.id) }
                    )
                }
            }

            AnimatedVisibility(
                visible = analyticsOpen,
                enter = fadeIn() + scaleIn(initialScale = 0.98f),
                exit = fadeOut() + scaleOut(targetScale = 0.98f),
                modifier = Modifier.fillMaxSize()
            ) {
                AnalyticsDashboard(
                    notes = notesList,
                    viewModel = viewModel,
                    onClose = { viewModel.analyticsOpen.value = false }
                )
            }
        }
    }
}

@Composable
fun AmbientOrb(
    color: Color,
    size: Dp,
    alpha: Float,
    alignment: Alignment = Alignment.TopStart,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    delayMs: Int = 0
) {
    // Generate a beautiful, glowing mesh orb fully in native Compose using Canvas radial brush
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(x = offsetX, y = offsetY)
                .size(size)
                .blur(48.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

// PREMIUM BUTTONS
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color = Color.White,
    borderColor: Color = Color.White.copy(alpha = 0.1f),
    backgroundColor: Color = Color.White.copy(alpha = 0.04f),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            content()
        }
    }
}

@Composable
fun GlassAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val bg = if (active) Color.White else Color.White.copy(alpha = 0.08f)
    val textC = if (active) Color.Black else Color.White
    val borderC = if (active) Color.White else Color.White.copy(alpha = 0.1f)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = textC
        ),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, borderC),
        contentPadding = PaddingValues(horizontal = 32.dp),
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textC
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            content()
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = Color.White.copy(alpha = 0.35f)
        ),
        modifier = modifier
    )
}

@Composable
fun Badge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = MutedText.copy(alpha = 0.8f)
            )
        )
    }
}

@Composable
fun MiniPill(
    active: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                if (active) Color.White else Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(24.dp)
            )
            .background(
                if (active) Color.White else Color.White.copy(alpha = 0.03f),
                RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = (-0.5).sp,
                color = if (active) Color.Black else Color.White.copy(alpha = 0.85f)
            )
        )
    }
}

// ONBOARDING
data class CinematicLine(val eyebrow: String, val title: String, val body: String)

val cinematicLines = listOf(
    CinematicLine(
        eyebrow = "Atomic headaches",
        title = "Write before the moment disappears.",
        body = "A private journal for fast capture. Timestamp, day, and location are attached automatically."
    ),
    CinematicLine(
        eyebrow = "Automatic context",
        title = "The note keeps its own memory.",
        body = "Time, date, and place stay with the entry. The writing stays at the center."
    ),
    CinematicLine(
        eyebrow = "Immediate entry",
        title = "What is on your mind right now?",
        body = "The editor opens ready. No setup. No explanation. Just the cursor."
    ),
    CinematicLine(
        eyebrow = "Subtle confirmation",
        title = "Saved at 17:20.",
        body = "A quiet confirmation, then back to the timeline."
    ),
    CinematicLine(
        eyebrow = "Timeline",
        title = "A private archive that gets out of the way.",
        body = "Recent notes arrive cleanly, with one strong action always in reach."
    )
)

val demoCards = listOf(
    Pair("Summer", "No items"),
    Pair("Routine", "4 items")
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingOverlay(
    onFinish: (String, String) -> Unit,
    playSound: (Float, Float) -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0: Curiosity, 1: Comfort, 2: Understanding, 3: First Action, 4: Satisfaction, 5: Belonging

    // Text values for first memory
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    // Breathing pulse for screen 0, 1 & 2
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val backdropColor by animateColorAsState(
        targetValue = when (step) {
            0 -> Color(0xFF030305)
            1 -> Color(0xFF06070B)
            2 -> Color(0xFF010102) // deep space blackout
            3 -> Color(0xFF07070D)
            4 -> Color(0xFF050A0B) // satisfying deep teal atmosphere
            else -> Color(0xFF030305)
        },
        animationSpec = tween(1200),
        label = "backdropColor"
    )

    Scaffold(
        containerColor = backdropColor,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            // Ambient organic moving elements in the background to feel like a "living space designed for thoughts"
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerPt = this.center
                val baseRadius = this.size.minDimension * 0.35f * pulseScale
                
                // Draw nested gradient breathing glow rings represent organic focus
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            when (step) {
                                0 -> NeonCyan.copy(alpha = 0.10f)
                                1 -> NeonViolet.copy(alpha = 0.10f)
                                2 -> Color.White.copy(alpha = 0.04f)
                                3 -> NeonCyan.copy(alpha = 0.10f)
                                4 -> NeonCyan.copy(alpha = 0.18f)
                                else -> Color.Transparent
                            },
                            Color.Transparent
                        ),
                        center = centerPt,
                        radius = baseRadius * 1.6f
                    ),
                    center = centerPt,
                    radius = baseRadius * 1.6f
                )

                // Thin elegant track circle representing timeline cycles
                drawCircle(
                    color = Color.White.copy(alpha = if (step == 2) 0.03f else 0.08f),
                    center = centerPt,
                    radius = baseRadius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                )

                // Transition Motion - Shared interactive context nodes (Comfort/Automatic Context step)
                if (step == 1) {
                    val angle1 = -40f * (Math.PI / 180f).toFloat()
                    val angle2 = 40f * (Math.PI / 180f).toFloat()
                    val angle3 = 180f * (Math.PI / 180f).toFloat()

                    val target1 = centerPt + androidx.compose.ui.geometry.Offset(
                        x = Math.cos(angle1.toDouble()).toFloat() * baseRadius * 1.25f,
                        y = Math.sin(angle1.toDouble()).toFloat() * baseRadius * 1.25f
                    )
                    val target2 = centerPt + androidx.compose.ui.geometry.Offset(
                        x = Math.cos(angle2.toDouble()).toFloat() * baseRadius * 1.25f,
                        y = Math.sin(angle2.toDouble()).toFloat() * baseRadius * 1.25f
                    )
                    val target3 = centerPt + androidx.compose.ui.geometry.Offset(
                        x = Math.cos(angle3.toDouble()).toFloat() * baseRadius * 1.25f,
                        y = Math.sin(angle3.toDouble()).toFloat() * baseRadius * 1.25f
                    )

                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = centerPt,
                        end = target1,
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = centerPt,
                        end = target2,
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = centerPt,
                        end = target3,
                        strokeWidth = 2f
                    )

                    drawCircle(color = NeonCyan, radius = 5.dp.toPx(), center = target1)
                    drawCircle(color = NeonViolet, radius = 5.dp.toPx(), center = target2)
                    drawCircle(color = CreamWhite, radius = 5.dp.toPx(), center = target3)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // top indicators bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    when (step) {
                                        0 -> NeonCyan
                                        1 -> NeonViolet
                                        2 -> Color.White.copy(alpha = 0.4f)
                                        3 -> NeonCyan
                                        4 -> CreamWhite
                                        else -> Color.White
                                    }, CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (step) {
                                0 -> "CURIOSITY"
                                1 -> "COMFORT"
                                2 -> "UNDERSTANDING"
                                3 -> "FIRST ACTION"
                                4 -> "SATISFACTION"
                                else -> "BELONGING"
                            },
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 2.sp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Text(
                        text = "9:41 AM",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }

                // animated multi-stage movie screen
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(800, easing = FastOutSlowInEasing)) +
                                    slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(800, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(animationSpec = tween(600)) +
                                    slideOutVertically(targetOffsetY = { -50 }, animationSpec = tween(600))
                        },
                        label = "cinematicTransition"
                    ) { currentStep ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (currentStep) {
                                0 -> {
                                    // 1. CURIOSITY
                                    Text(
                                        text = "Atomic Headaches",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 32.sp,
                                            letterSpacing = (-1).sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "A quiet retreat for deep thoughts.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 18.sp,
                                            lineHeight = 26.sp,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(30.dp))
                                    Text(
                                        text = "Let the distractions dissolve.\nFocus is a practice, not a feature.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp,
                                            color = CreamWhite.copy(alpha = 0.45f)
                                        )
                                    )
                                }
                                1 -> {
                                    // 2. COMFORT
                                    Text(
                                        text = "Context stays with the memory.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            letterSpacing = (-0.5).sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Time, day, and location are captured in absolute silence, attaching a permanent context to every text.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 15.sp,
                                            lineHeight = 24.sp,
                                            color = Color.White.copy(alpha = 0.65f)
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(30.dp))
                                    
                                    // Elegant floating contextual cards
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.03f))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.AccessTime, "Time", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("3:41 PM", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White))
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.03f))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.CalendarToday, "Day", tint = NeonViolet, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Saturday", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White))
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.03f))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.LocationOn, "Location", tint = CreamWhite, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Vancouver", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White))
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // 3. UNDERSTANDING
                                    Text(
                                        text = "The cursor is waiting.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            letterSpacing = (-0.5).sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No barriers. No categories or folders.\nJust pure space to think.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(30.dp))
                                    
                                    // A mockup of a blinking cursor inside zero-distraction layout
                                    val cursorIntensity = rememberInfiniteTransition(label = "cursor")
                                    val alphaVal by cursorIntensity.animateFloat(
                                        initialValue = 0.1f,
                                        targetValue = 1.0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                    Text(
                                        text = "―",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 32.sp,
                                            color = CreamWhite.copy(alpha = alphaVal)
                                        )
                                    )
                                }
                                3 -> {
                                    // 4. FIRST ACTION
                                    Text(
                                        text = "What is on your mind?",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Type your first memory right here.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(fontSize = 13.sp, color = MutedText)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Minimal elegant draft input block
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.82f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            BasicTextField(
                                                value = noteTitle,
                                                onValueChange = { noteTitle = it },
                                                textStyle = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 17.sp,
                                                    color = Color.White
                                                ),
                                                cursorBrush = SolidColor(CreamWhite),
                                                decorationBox = { innerTextField ->
                                                    if (noteTitle.isEmpty()) {
                                                        Text("Title (Optional)", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White.copy(alpha = 0.3f)))
                                                    }
                                                    innerTextField()
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(Color.White.copy(alpha = 0.08f))
                                            )

                                            BasicTextField(
                                                value = noteBody,
                                                onValueChange = { noteBody = it },
                                                textStyle = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 14.sp,
                                                    lineHeight = 22.sp,
                                                    color = Color.White.copy(alpha = 0.9f)
                                                ),
                                                cursorBrush = SolidColor(CreamWhite),
                                                decorationBox = { innerTextField ->
                                                    if (noteBody.isEmpty()) {
                                                        Text("Begin writing your first thought...", style = TextStyle(fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f)))
                                                    }
                                                    innerTextField()
                                                },
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                                            )
                                        }
                                    }
                                }
                                4 -> {
                                    // 5. SATISFACTION
                                    Text(
                                        text = "Thought preserved.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 26.sp,
                                            color = CreamWhite
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Your first memory is safely locked into the private local archive, with automated context parameters firmly attached.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 15.sp,
                                            lineHeight = 24.sp,
                                            color = Color.White.copy(alpha = 0.65f)
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                                else -> {
                                    // 6. BELONGING
                                    Text(
                                        text = "Welcome to your space.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 26.sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "A living interface designed solely for thoughts. Complete your overture to enter.",
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 15.sp,
                                            lineHeight = 24.sp,
                                            color = Color.White.copy(alpha = 0.65f)
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    )
                                }
                            }
                        }
                    }
                }

                // dots indicators & next button
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0..5) {
                            Box(
                                modifier = Modifier
                                    .size(width = if (i == step) 20.dp else 6.dp, height = 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == step) CreamWhite else Color.White.copy(alpha = 0.15f)
                                    )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Back/Skip button
                        if (step > 0 && step < 5) {
                            Text(
                                text = "BACK",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        playSound(650f, 0.1f)
                                        step--
                                    }
                                    .padding(12.dp)
                            )
                        } else {
                            Text(
                                text = "SKIP",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        playSound(600f, 0.1f)
                                        step = 5
                                    }
                                    .padding(12.dp)
                                    .testTag("onboarding_skip")
                            )
                        }

                        // Right CTA Next button
                        Button(
                            onClick = {
                                if (step == 3) {
                                    playSound(1000f, 0.2f)
                                    step = 4
                                } else if (step == 4) {
                                    playSound(850f, 0.15f)
                                    step = 5
                                } else if (step == 5) {
                                    playSound(1100f, 0.3f)
                                    onFinish(noteTitle, noteBody)
                                } else {
                                    playSound(800f, 0.15f)
                                    step++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CreamWhite,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("onboarding_next_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = when (step) {
                                        3 -> "PRESERVE"
                                        4 -> "CONTINUE"
                                        5 -> "ENTER JOURNAL"
                                        else -> "NEXT"
                                    },
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// MAIN TIMELINE SCREEN
@Composable
fun MainStream(
    viewModel: NoteViewModel,
    notesList: List<Note>
) {
    val search by viewModel.search
    val filter by viewModel.filter
    val focusMode by viewModel.focusMode
    val syncPulse by viewModel.syncPulse

    val context = LocalContext.current
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var showBackupsDialog by remember { mutableStateOf(false) }

    // Accelerometer Motion Sensation
    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) }

    DisposableEffect(sensorManager, accelerometer) {
        if (accelerometer != null) {
            val listener = object : android.hardware.SensorEventListener {
                private var motionScore = 0f

                override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                    if (event == null) return
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                    val diff = kotlin.math.abs(magnitude - 9.81f)

                    motionScore = motionScore * 0.95f + diff * 0.05f
                    val isMoving = motionScore > 0.6f
                    if (viewModel.walkingDetected.value != isMoving) {
                        viewModel.walkingDetected.value = isMoving
                    }
                }

                override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(viewModel.walkingDetected.value) {
        if (viewModel.walkingDetected.value) {
            if (!viewModel.composerOpen.value && viewModel.draft.value == null) {
                viewModel.openNewNote("Transit")
                kotlinx.coroutines.delay(400)
                val currentDraft = viewModel.draft.value
                if (currentDraft != null && !viewModel.isRecording.value) {
                    val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasAudioPermission) {
                        viewModel.startRecording(currentDraft.id, context)
                    }
                }
            }
        }
    }



    // Setup Intent Launcher for recoverable Google Authentication dialogs
    val authResolutionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.performDriveSync(context)
        }
    }

    // Setup Choose Document launcher to import backup files
    val importLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.importBookFromFile(context, uri) { succeeded ->
                if (succeeded) {
                    android.widget.Toast.makeText(context, "Journal Book imported successfully!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Import failed: File format not recognized.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showProfileSettingsDialog) {
        AccountSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showProfileSettingsDialog = false },
            onTriggerImport = {
                showProfileSettingsDialog = false
                importLauncher.launch("application/json")
            },
            onTriggerResolution = { intent ->
                authResolutionLauncher.launch(intent)
            },
            playSound = { f, d -> viewModel.playSound(f, d) }
        )
    }

    if (showBackupsDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showBackupsDialog = false }
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        tint = CreamWhite,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "JOURNAL PORTABILITY",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CreamWhite,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Export your lifetime journal records as a secure JSON backup, or import an existing data file directly into this device.",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = MutedText,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Export Button
                    Button(
                        onClick = {
                            viewModel.playSound(800f, 0.12f)
                            showBackupsDialog = false
                            viewModel.exportBookToFile(context) { error ->
                                if (error != null) {
                                    android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Journal backup successful!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            "Export Lifetime Backup (.json)",
                            style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                        )
                    }

                    // Import Button
                    OutlinedButton(
                        onClick = {
                            viewModel.playSound(800f, 0.12f)
                            showBackupsDialog = false
                            importLauncher.launch("application/json")
                        },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            "Import Backup File (.json)",
                            style = TextStyle(fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 13.sp)
                        )
                    }

                    // Close Button
                    TextButton(
                        onClick = {
                            viewModel.playSound(500f, 0.10f)
                            showBackupsDialog = false
                        }
                    ) {
                        Text("Dismiss", style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp))
                    }
                }
            }
        }
    }

    // Process filters and query matching useMemo counterparts
    val filteredNotes = remember(notesList, search, filter) {
        val lower = search.trim().lowercase()
        notesList.filter { note ->
            val matchesSearch = lower.isEmpty() ||
                "${note.title} ${note.body} ${note.locationLabel} ${note.mood} ${note.preset}".lowercase().contains(lower)
            val matchesFilter = when (filter) {
                "All" -> true
                "Today" -> viewModel.getDaysDiff(note.createdAt) == 0
                "This Week" -> viewModel.getDaysDiff(note.createdAt) <= 7
                "This Month" -> viewModel.getDaysDiff(note.createdAt) <= 30
                "This Year" -> viewModel.getDaysDiff(note.createdAt) <= 365
                "Lifetime" -> true
                "Mood" -> note.mood.isNotEmpty()
                else -> note.locationLabel == filter || note.mood == filter
            }
            matchesSearch && matchesFilter
        }
    }

    // Grouping
    val groupedNotes = remember(filteredNotes) {
        val map = linkedMapOf<String, MutableList<Note>>()
        filteredNotes.forEach { note ->
            val label = viewModel.groupLabel(note.createdAt)
            if (!map.containsKey(label)) {
                map[label] = mutableListOf()
            }
            map[label]?.add(note)
        }
        map.entries.toList()
    }

    val latestNote = notesList.firstOrNull()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable {
                        viewModel.playSound(950f, 0.15f)
                        viewModel.openNewNote()
                    }
                    .testTag("floating_add_note_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create static or voice note entries",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Status row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(NeonCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "09:41 AM",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Book Reader Icon Trigger
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Read journal notes like a book",
                        tint = CreamWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                viewModel.playSound(850f, 0.15f)
                                viewModel.bookReaderOpen.value = true
                            }
                            .testTag("read_book_trigger")
                    )

                    // Profile / Sync Settings Toggle
                    val isAuthCompleted by viewModel.authCompleted
                    Icon(
                        imageVector = if (isAuthCompleted) Icons.Default.CloudQueue else Icons.Default.AccountCircle,
                        contentDescription = "Verify Google accounts or local shares",
                        tint = if (isAuthCompleted) NeonCyan else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                viewModel.playSound(650f, 0.12f)
                                showProfileSettingsDialog = true
                            }
                            .testTag("sync_settings_trigger")
                    )

                    // Import / Export backup trigger
                    Icon(
                        imageVector = Icons.Default.ImportExport,
                        contentDescription = "Import or export lifetime journal data",
                        tint = CreamWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                viewModel.playSound(850f, 0.15f)
                                showBackupsDialog = true
                            }
                            .testTag("backup_import_export_trigger")
                    )

                    // Audio Toggle
                    val soundOn by viewModel.audioState
                    @Suppress("DEPRECATION")
                    Icon(
                        imageVector = if (soundOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = "Toggle Soundtrack",
                        tint = if (soundOn) CreamWhite else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { viewModel.toggleGlobalAudio() }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(5.dp).background(Color.White.copy(alpha = 0.7f), CircleShape))
                        Box(modifier = Modifier.size(width = 20.dp, height = 5.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.size(width = 12.dp, height = 5.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp)))
                    }
                }
            }

            // Stream Title Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                            contentDescription = "Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        SectionLabel("ATOMIC HEADACHES")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (focusMode) "Focus mode" else "Notes",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (focusMode) "One primary action. Everything else steps back."
                        else if (notesList.isEmpty()) "Start small. Keep the signal."
                        else "One more line is enough.",
                        style = TextStyle(fontWeight = FontWeight.Light, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Filter list trigger
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                viewModel.playSound(600f, 0.12f)
                                viewModel.filter.value = if (viewModel.filter.value == "All") "Today" else "All"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FilterList, null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(18.dp))
                    }

                    // Plus composer trigger
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, CreamWhite.copy(alpha = 0.3f), CircleShape)
                            .background(CreamWhite.copy(alpha = 0.1f))
                            .clickable {
                                viewModel.openNewNote("Home")
                            }
                            .testTag("new_note_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = CreamWhite, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Filters selector row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                items(FILTERS) { item ->
                    MiniPill(
                        active = filter == item,
                        text = item,
                        onClick = {
                            viewModel.playSound(650f, 0.12f)
                            if (item == "Analytics") {
                                viewModel.analyticsOpen.value = true
                            } else {
                                viewModel.filter.value = item
                            }
                        }
                    )
                }
            }

            // Search Bar Component
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = search,
                    onValueChange = { viewModel.search.value = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Light),
                    singleLine = true,
                    cursorBrush = SolidColor(CreamWhite),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input"),
                    decorationBox = { innerTextField ->
                        if (search.isEmpty()) {
                            Text(
                                "Search notes, mood, location...",
                                style = TextStyle(color = Color.White.copy(alpha = 0.2f), fontSize = 15.sp, fontWeight = FontWeight.Light)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Metric tracking stats
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredNotes.size} ARTIFACTS",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.4f))
                )
                Text(
                    text = if (syncPulse > 0) "SYNCED [$syncPulse]" else "READY",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.4f))
                )
            }

            // Note List Items Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                if (notesList.isEmpty()) {
                    // Empty Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SectionLabel("EMPTY STATE")
                            Text("Begin first entry.", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White.copy(alpha = 0.9f)))
                            Text(
                                "The app is ready. Timestamp, day, and location will attach automatically.",
                                style = TextStyle(fontWeight = FontWeight.Light, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LOCATIONS.take(3).forEach { loc ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                            .clickable { viewModel.openNewNote(loc) }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(loc.uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)))
                                    }
                                }
                            }
                        }
                    }
                } else if (groupedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("No matches.", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White.copy(alpha = 0.9f)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Change the filter or search terms.", style = TextStyle(fontWeight = FontWeight.Light, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f)))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        groupedNotes.forEach { (group, items) ->
                            item {
                                Text(
                                    text = group.uppercase(),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 3.sp,
                                        color = CreamWhite
                                    ),
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                )
                            }
                            items(items) { note ->
                                NoteCard(
                                    note = note,
                                    viewModel = viewModel,
                                    onOpen = { viewModel.detailOpenId.value = note.id }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom capture tray block
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(30.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            SectionLabel("CURRENT CAPTURE LAYER")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (latestNote != null) viewModel.formatTime(latestNote.createdAt) else "17:20",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = CreamWhite)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (latestNote != null) "${latestNote.title.ifEmpty { "Untitled" }} · ${latestNote.locationLabel}".uppercase() else "LEGS FOCUS · IN PROGRESS",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { viewModel.openNewNote("Home") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowOutward, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    viewModel.playSound(650f, 0.12f)
                                    viewModel.focusMode.value = !viewModel.focusMode.value
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (focusMode) "Exit focus" else "Enter focus",
                                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.8f))
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable { viewModel.openNewNote("Home") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "New note",
                                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
            }
        }
    }
}

// NOTE CARD timeline item representation
@Composable
fun NoteCard(
    note: Note,
    viewModel: NoteViewModel,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.02f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title.ifEmpty { "Drawing" },
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.5).sp, color = Color.White.copy(alpha = 0.95f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${viewModel.formatTime(note.createdAt)}  ·  ${viewModel.groupLabel(note.createdAt)}  ·  ${note.locationLabel}".uppercase(),
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.40f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .background(Color.White.copy(alpha = 0.04f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowOutward, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = note.body.ifEmpty { "Empty draft item" },
                style = TextStyle(fontWeight = FontWeight.Light, fontSize = 15.sp, lineHeight = 22.sp, color = Color.White.copy(alpha = 0.7f)),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (note.mood.isNotEmpty() || note.preset.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.mood.isNotEmpty()) {
                        Badge(text = note.mood)
                    }
                    if (note.preset.isNotEmpty()) {
                        Badge(text = note.preset)
                    }
                }
            }
        }
    }
}

// COMPOSER SHEET (Write, Context, Mood tabs)
@Composable
fun ComposerSheet(
    draft: Note,
    viewModel: NoteViewModel,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableStateOf("write") }
    val canSave = draft.title.trim().isNotEmpty() || draft.body.trim().isNotEmpty()

    val context = LocalContext.current
    val isListening = viewModel.isRecording.value

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording(draft.id, context)
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleListening() {
        if (viewModel.isRecording.value) {
            viewModel.stopRecordingAndSave(draft.id)
        } else {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.startRecording(draft.id, context)
            } else {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(viewModel.walkingDetected.value) {
        if (viewModel.walkingDetected.value && !viewModel.isRecording.value && draft.body.isEmpty() && draft.title.isEmpty()) {
            toggleListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudio()
        }
    }

    Scaffold(
        containerColor = SlateBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("WORKSPACE CONTEXT")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("FOCUS DRAFT", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White))
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Tabs navigator row
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("write", "Write"),
                    Pair("context", "Context"),
                    Pair("tone", "Mood")
                ).forEach { (key, name) ->
                    val active = activeTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (active) Color.White else Color.Transparent)
                            .clickable {
                                viewModel.playSound(650f, 0.12f)
                                activeTab = key
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            style = TextStyle(
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (active) Color.Black else Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            // Main Tab Workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                when (activeTab) {
                    "write" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Title block
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("PROMPT / HEADER", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(10.dp))
                                BasicTextField(
                                    value = draft.title,
                                    onValueChange = { viewModel.updateDraftTitle(it) },
                                    textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                    singleLine = true,
                                    cursorBrush = SolidColor(CreamWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("draft_title_input"),
                                    decorationBox = { innerTextField ->
                                        if (draft.title.isEmpty()) {
                                            Text(
                                                "What is on your mind?",
                                                style = TextStyle(color = Color.White.copy(alpha = 0.2f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "CURSOR ACTIVE. FOCUS ON LOGICAL SUBSTANCE.",
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f))
                                )
                            }

                            // Body block
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ENTRY MATRIX", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // "DEVIATE TO GPS" button
                                        androidx.compose.material3.Button(
                                            onClick = {
                                                viewModel.playSound(800f, 0.11f)
                                                viewModel.useDeviceLocation()
                                            },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = Color.White.copy(alpha = 0.05f),
                                                contentColor = CreamWhite
                                            ),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp).testTag("use_gps_location_button")
                                        ) {
                                            Text(
                                                "DEVIATE TO GPS",
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        // "Save note" button - Prominent custom check/save button
                                        androidx.compose.material3.Button(
                                            onClick = {
                                                if (canSave) onSave()
                                            },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF8B5CF6),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp).testTag("save_note_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Save Note",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = Color.White
                                                )
                                                Text(
                                                    "SAVE NOTE",
                                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                BasicTextField(
                                    value = draft.body,
                                    onValueChange = { viewModel.updateDraftBody(it) },
                                    textStyle = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.Light, lineHeight = 24.sp),
                                    cursorBrush = SolidColor(CreamWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp)
                                        .testTag("draft_body_input"),
                                    decorationBox = { innerTextField ->
                                        if (draft.body.isEmpty()) {
                                            Text(
                                                "Begin capturing raw details. Keep the noise low.",
                                                style = TextStyle(color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp, fontWeight = FontWeight.Light)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            // Original Voice file display & player
                            if (!draft.audioPath.isNullOrEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("ORIGINAL AUDIO PRESERVED", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val isPlaying = viewModel.isPlayingAudio.value && viewModel.playingAudioPath.value == draft.audioPath
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text("Voice log backup is safe", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CreamWhite))
                                                Text("Tap arrow to play back entry", style = TextStyle(fontSize = 11.sp, color = MutedText))
                                            }
                                        }
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalIconButton(
                                                onClick = { 
                                                    draft.audioPath?.let { viewModel.toggleAudioPlay(it) } 
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = if (isPlaying) "Stop playback" else "Play recorded audio"
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Voice dictation & capture ring
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SectionLabel("VOICE TRANSCRIBER")
                                
                                val durationVal = viewModel.recordingDuration.value
                                
                                val pulseScale by animateFloatAsState(
                                    targetValue = if (isListening) 1.25f else 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "mic_pulse"
                                )

                                // RECORD ACTION PANEL
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .scale(if (isListening) pulseScale else 1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isListening) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isListening) NeonCyan else Color.White.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.playSound(850f, 0.15f)
                                            toggleListening()
                                        }
                                        .testTag("onboarding_mic_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                        contentDescription = "Tap to speak",
                                        tint = if (isListening) NeonCyan else CreamWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isListening) {
                                            val m = durationVal / 60
                                            val s = durationVal % 60
                                            val timeFmt = String.format("%02d:%02d", m, s)
                                            "RECORDING ACTIVE [$timeFmt] • SPEAK TO JOURNAL"
                                        } else {
                                            "TAP TO START HIGH-FIDELITY VOICE BACKUP RECORD"
                                        },
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = if (isListening) NeonCyan else Color.White.copy(alpha = 0.4f)),
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    // Waves representation during listening
                                    if (isListening) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp).height(24.dp)
                                        ) {
                                            viewModel.amplitudeList.takeLast(16).forEach { amp ->
                                                val barHeight = (amp / 8000f).coerceIn(2f, 24f).dp
                                                Box(
                                                    modifier = Modifier
                                                        .padding(horizontal = 2.dp)
                                                        .width(4.dp)
                                                        .height(barHeight)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(NeonCyan)
                                                )
                                            }
                                        }
                                    }

                                    if (viewModel.walkingDetected.value) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "🚶 Walking motion active (Auto-routing mode)",
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonViolet, fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // If recorded audio backup exists, show a local media player card!
                                val audioPath = draft.audioPath
                                if (!isListening && !audioPath.isNullOrEmpty()) {
                                    val isPlayingThis = viewModel.isPlayingAudio.value && viewModel.playingAudioPath.value == audioPath
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isPlayingThis) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                                    .border(1.dp, if (isPlayingThis) NeonCyan else Color.White.copy(alpha = 0.15f), CircleShape)
                                                    .clickable { viewModel.toggleAudioPlay(audioPath) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Playback local backup audio",
                                                    tint = if (isPlayingThis) NeonCyan else CreamWhite,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "AUDIO SEQUENCE SAVED",
                                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    "Local backup audio is available on-disk",
                                                    style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "context" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("CHRONOLOGY", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(viewModel.formatTime(draft.createdAt), style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = CreamWhite))
                                Text(viewModel.formatDay(draft.createdAt).uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)))
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("GEOGRAPHICAL MAP", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LOCATIONS.take(3).forEach { loc ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (draft.locationLabel == loc) Color.White else Color.White.copy(alpha = 0.05f)
                                                )
                                                .clickable {
                                                    viewModel.playSound(650f, 0.12f)
                                                    viewModel.updateDraftLocation(loc)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                loc,
                                                style = TextStyle(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (draft.locationLabel == loc) Color.Black else Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LOCATIONS.drop(3).forEach { loc ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (draft.locationLabel == loc) Color.White else Color.White.copy(alpha = 0.05f)
                                                )
                                                .clickable {
                                                    viewModel.playSound(650f, 0.12f)
                                                    viewModel.updateDraftLocation(loc)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                loc,
                                                style = TextStyle(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (draft.locationLabel == loc) Color.Black else Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = CreamWhite, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = (if (draft.deviceLocation.isNotEmpty()) "${draft.locationLabel} · ${draft.deviceLocation}" else draft.locationLabel).uppercase(),
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                    )
                                }
                            }

                            // Custom Location Option input
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("CUSTOM LOCATION", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(10.dp))
                                BasicTextField(
                                    value = draft.locationLabel,
                                    onValueChange = { viewModel.updateDraftLocation(it) },
                                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                    singleLine = true,
                                    cursorBrush = SolidColor(CreamWhite),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        if (draft.locationLabel.isEmpty()) {
                                            Text(
                                                "Or specify custom location here...",
                                                style = TextStyle(color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("RECENCY INDICES", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("just now", "today", "yesterday").forEach { tag ->
                                        Badge(text = tag)
                                    }
                                }
                            }
                        }
                    }
                    "tone" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("SENSORY MOOD CHIP", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(12.dp))
                                MOODS.chunked(3).forEach { rowMoods ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowMoods.forEach { mood ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(
                                                        if (draft.mood == mood) Color.White else Color.White.copy(alpha = 0.05f)
                                                    )
                                                    .clickable {
                                                        viewModel.playSound(650f, 0.12f)
                                                        viewModel.updateDraftMood(mood)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    mood,
                                                    style = TextStyle(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (draft.mood == mood) Color.Black else Color.White
                                                    )
                                                )
                                            }
                                        }
                                        if (rowMoods.size < 3) {
                                            Spacer(modifier = Modifier.weight((3 - rowMoods.size).toFloat()))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "OPTIONAL ATTRIBUTE. ZERO SETUP REQUIRED.",
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                                )
                            }

                            // Custom Mood Option input
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("CUSTOM MOOD", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(10.dp))
                                BasicTextField(
                                    value = draft.mood,
                                    onValueChange = { viewModel.updateDraftMood(it) },
                                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                    singleLine = true,
                                    cursorBrush = SolidColor(CreamWhite),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        if (draft.mood.isEmpty()) {
                                            Text(
                                                "Or specify custom mood here...",
                                                style = TextStyle(color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("METRIC ENVELOPE PREVIEW", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = draft.mood.ifEmpty { "Unset" }.uppercase(),
                                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CreamWhite)
                                        )
                                        Text(
                                            text = "Synchronized tone model",
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.40f))
                                        )
                                    }
                                    Icon(Icons.Default.NightsStay, null, tint = NeonViolet, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }


        }
    }
}

// NOTE DETAIL VIEW
@Composable
fun DetailSheet(
    note: Note,
    viewModel: NoteViewModel,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        containerColor = SlateBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("LOGGED ARTIFACT")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.title.ifEmpty { "Drawing" },
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White.copy(alpha = 0.95f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Body Display
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    Text("DRAFT CONTENT", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = note.body.ifEmpty { "No content." },
                        style = TextStyle(fontWeight = FontWeight.Light, fontSize = 16.sp, lineHeight = 24.sp, color = Color.White.copy(alpha = 0.8f))
                    )
                }

                if (!note.audioPath.isNullOrEmpty()) {
                    val isPlaying = viewModel.isPlayingAudio.value && viewModel.playingAudioPath.value == note.audioPath
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (isPlaying) NeonCyan else Color.White.copy(alpha = 0.1f), CircleShape)
                                        .clickable { note.audioPath?.let { viewModel.toggleAudioPlay(it) } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause backup note audio" else "Play backup note audio",
                                        tint = if (isPlaying) NeonCyan else CreamWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "ORIGINAL AUDIO ARCHIVE",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isPlaying) "Playing recording..." else "Buffered locally on device",
                                        style = TextStyle(fontSize = 12.sp, color = CreamWhite)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Text("GPS ANCHOR", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(note.locationLabel.uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CreamWhite))
                        if (note.deviceLocation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(note.deviceLocation, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.3f)))
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Text("SENSORY PARAM", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(note.mood.ifEmpty { "Unset" }.uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NeonViolet))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TIMEFRAME", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                    Text(
                        text = "${viewModel.formatTime(note.createdAt)}  ·  ${viewModel.formatDay(note.createdAt)}".uppercase(),
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    )
                }
            }

            // Bottom Actions Slate
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassAction(
                    onClick = onEdit,
                    icon = Icons.Default.Edit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_note_button")
                ) {
                    Text("Edit", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp))
                }

                GlassAction(
                    onClick = onDelete,
                    icon = Icons.Default.Delete,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.Red.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                        .testTag("delete_note_button")
                ) {
                    Text("Delete", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp, color = Color.Red.copy(alpha = 0.8f)))
                }
            }
        }
    }
}

// ANALYTICS PANEL
@Composable
fun AnalyticsDashboard(
    notes: List<Note>,
    viewModel: NoteViewModel,
    onClose: () -> Unit
) {
    val total = notes.size
    val topLocation = remember(notes) {
        notes.groupBy { it.locationLabel }
            .maxByOrNull { it.value.size }?.key ?: "None"
    }
    val topMood = remember(notes) {
        notes.filter { it.mood.isNotEmpty() }
            .groupBy { it.mood }
            .maxByOrNull { it.value.size }?.key ?: "None"
    }

    Scaffold(
        containerColor = SlateBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("SYSTEM ANALYTICS")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("METRICS DECK", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White))
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Metrics Cards Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Composed Artifacts", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$total", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = CreamWhite))
                        }
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.TrendingUp, null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        Text("Dominant Anchor", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(topLocation.uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CreamWhite))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        Text("Prevailing Mood", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(topMood.uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = NeonViolet))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    SectionLabel("CHRONOLOGICAL HISTORY")
                    Spacer(modifier = Modifier.height(16.dp))
                    if (notes.isEmpty()) {
                        Text("No data logged.", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f)))
                    } else {
                        notes.take(5).forEachIndexed { i, note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.formatDay(note.createdAt).uppercase(),
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                )
                                Text(
                                    text = note.title.ifEmpty { "Drawing" },
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White.copy(alpha = 0.82f)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (i < 4 && i < notes.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.04f))
                                )
                            }
                        }
                    }
                }
            }

            // Back Action button
            Spacer(modifier = Modifier.height(16.dp))
            GlassAction(
                onClick = onClose,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Stream", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp))
            }
        }
    }
}

// ----------------------------------------------------
// ACCOUNT SETTINGS DIALOG (GOOGLE SYNC & IMPORT/EXPORT)
// ----------------------------------------------------
@Composable
fun AccountSettingsDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onTriggerImport: () -> Unit,
    onTriggerResolution: (android.content.Intent) -> Unit,
    playSound: (Float, Float) -> Unit
) {
    val context = LocalContext.current
    val authCompleted by viewModel.authCompleted
    val userEmail by viewModel.userEmail
    val userDisplayName by viewModel.userDisplayName
    val syncStatus by viewModel.syncStatus

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Cloud Sync & Portability",
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (authCompleted) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CreamWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (userDisplayName ?: "K").take(1).uppercase(),
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                )
                            }
                            Column {
                                Text(
                                    userDisplayName ?: "Journal Keeper",
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                )
                                Text(
                                    userEmail ?: "",
                                    style = TextStyle(fontSize = 12.sp, color = MutedText)
                                )
                            }
                        }
                    }

                    if (syncStatus.isNotEmpty()) {
                        Text(
                            text = syncStatus,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CreamWhite),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            playSound(950f, 0.15f)
                            viewModel.performDriveSync(context) { intent ->
                                onTriggerResolution(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreamWhite, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sync_drive_now")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(16.dp))
                            Text("Sync with Google Drive", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    "Guest Account (No Cloud Sync)",
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Your notes are only stored on this device. Deleting the app deletes all of your data.",
                                    style = TextStyle(fontSize = 11.sp, color = MutedText)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            playSound(800f, 0.15f)
                            onDismiss()
                            viewModel.sharedPrefs.edit().remove("auth_skipped").apply()
                            viewModel.authSkipped.value = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreamWhite, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_google_link")
                    ) {
                        Text("Connect Google Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            playSound(700f, 0.12f)
                            viewModel.exportBookToFile(context) { error ->
                                if (error != null) {
                                    android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Journal exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("dialog_export_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                            Text("Export JSON", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            playSound(700f, 0.12f)
                            onTriggerImport()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("dialog_import_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(14.dp))
                            Text("Import JSON", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB74D).copy(alpha = 0.15f),
                        contentColor = Color(0xFFFFB74D)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Made with love by Pickko", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp))
                        Icon(Icons.Default.Coffee, contentDescription = "Support", modifier = Modifier.size(18.dp))
                    }
                }

                if (authCompleted) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                    TextButton(
                        onClick = {
                            playSound(300f, 0.25f)
                            viewModel.logOut(context)
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally).testTag("dialog_logout_button")
                    ) {
                        Text("Sign Out Account", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_close")) {
                Text("Close", color = CreamWhite, fontSize = 13.sp)
            }
        },
        containerColor = SlateCard,
        titleContentColor = Color.White,
        textContentColor = MutedText
    )
}
