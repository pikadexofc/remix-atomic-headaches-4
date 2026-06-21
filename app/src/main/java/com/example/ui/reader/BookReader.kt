package com.example.ui.reader

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.CreamWhite
import com.example.ui.MutedText
import com.example.ui.SlateBg
import com.example.ui.SlateCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data structure representing a page in our visual book
data class BookPage(
    val noteId: String,
    val dateStr: String,
    val title: String,
    val bodySection: String,
    val location: String = "",
    val mood: String = "",
    val isContinuation: Boolean = false
)

@Composable
fun BookReader(
    notes: List<Note>,
    onClose: () -> Unit,
    playSound: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Paginate current notes
    val bookPages = remember(notes) { paginateNotesIntoPages(notes) }
    var currentPageIndex by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBg),
        contentAlignment = Alignment.Center
    ) {
        // High fidelity outer bookshelf layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Elegant Book Title Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = CreamWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "THE CHRONICLES",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CreamWhite,
                            letterSpacing = 2.sp
                        )
                    )
                }

                // Bookmark Exit Button
                IconButton(
                    onClick = {
                        playSound(500f, 0.15f)
                        onClose()
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.04f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .size(36.dp)
                        .testTag("close_book_reader")
                ) {
                    Icon(Icons.Default.Close, "Close book", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            if (bookPages.isEmpty()) {
                // Empty Book Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .background(SlateCard, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "An Empty Leaf",
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Write your first journal entry to bind your very first book page.",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = MutedText,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                // Main Book Container (Gives authentic booklet / journal shape)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .shadow(16.dbShadow(), RoundedCornerShape(12.dp))
                        .border(4.dp, Color(0xFF2C241E), RoundedCornerShape(12.dp)) // Visual leather/cardboard dark cover rim
                        .background(Color(0xFFFCFAF2), RoundedCornerShape(12.dp)) // Parchment/Warm cream page body
                ) {
                    // Left-hand page fold accent giving curved depth along margins
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(18.dp)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Black.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Right-hand book curve/shadow edge
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(24.dp)
                            .align(Alignment.CenterEnd)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.02f),
                                        Color.Black.copy(alpha = 0.12f)
                                    )
                                )
                            )
                    )

                    // Actual page sheet containing elegant compact text content
                    val page = bookPages[currentPageIndex]
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            if (currentPageIndex > bookPages.indexOf(initialState)) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        label = "page_transition"
                    ) { activePage ->
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Decorative Date Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = activePage.dateStr.uppercase(),
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF6E5643), // Walnut ink color
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    if (activePage.mood.isNotEmpty() || activePage.location.isNotEmpty()) {
                                        Text(
                                            text = listOfNotNull(
                                                activePage.mood.ifEmpty { null },
                                                activePage.location.ifEmpty { null }
                                            ).joinToString(" • ").uppercase(),
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = Color(0xFF8F7A66)
                                            )
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFE5DBC7), thickness = 1.dp)

                                // Article Title
                                if (activePage.title.isNotEmpty()) {
                                    Text(
                                        text = activePage.title,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = Color(0xFF281C10), // Antique black
                                            lineHeight = 24.sp
                                        )
                                    )
                                } else {
                                    Text(
                                        text = "Untitled Entry",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF8F7A66)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Journal Body Text (Elegant serif typewriter/compact layout)
                                Text(
                                    text = activePage.bodySection,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = Color(0xFF382F25), // Chocolate charcoal text
                                        lineHeight = 21.sp,
                                        textAlign = TextAlign.Justify
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Book Bookmark page indicators
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = Color(0xFF8C3428), // Deep ribbon bookmark crimson red
                                    modifier = Modifier.size(24.dp)
                                )

                                Text(
                                    text = "Page ${currentPageIndex + 1} of ${bookPages.size}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF6E5643)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Book Navigation Controls at Bottom
            if (bookPages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back page trigger
                    TextButton(
                        onClick = {
                            if (currentPageIndex > 0) {
                                playSound(400f, 0.12f)
                                currentPageIndex--
                            }
                        },
                        enabled = currentPageIndex > 0,
                        colors = ButtonDefaults.textButtonColors(contentColor = CreamWhite),
                        modifier = Modifier.testTag("book_prev_page")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, modifier = Modifier.size(20.dp))
                            Text("PREVIOUS", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }

                    // Progress Marker Dot Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val maxDots = 5
                        val middle = maxDots / 2
                        val totalPages = bookPages.size
                        
                        val startDot = (currentPageIndex - middle).coerceIn(0, (totalPages - maxDots).coerceAtLeast(0))
                        val endDot = (startDot + maxDots - 1).coerceAtMost(totalPages - 1)

                        for (i in startDot..endDot) {
                            val activeVal = i == currentPageIndex
                            Box(
                                modifier = Modifier
                                    .size(if (activeVal) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(if (activeVal) CreamWhite else Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        playSound(600f, 0.1f)
                                        currentPageIndex = i
                                    }
                            )
                        }
                    }

                    // Next page trigger
                    TextButton(
                        onClick = {
                            if (currentPageIndex < bookPages.size - 1) {
                                playSound(420f, 0.12f)
                                currentPageIndex++
                            }
                        },
                        enabled = currentPageIndex < bookPages.size - 1,
                        colors = ButtonDefaults.textButtonColors(contentColor = CreamWhite),
                        modifier = Modifier.testTag("book_next_page")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("NEXT", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// Custom extension to format shadow Dp cleanly
private fun Int.dbShadow() = this.dp

// Paginating note text into comfortable, compact paragraphs chronologically
fun paginateNotesIntoPages(notes: List<Note>): List<BookPage> {
    val pages = mutableListOf<BookPage>()
    // Chronological order - oldest entries first represent standard physical reading journals!
    val sortedNotes = notes.sortedBy { it.createdAt }
    val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())

    sortedNotes.forEach { note ->
        val dateStr = sdf.format(Date(note.createdAt))
        val rawBody = note.body
        val textLimit = 650 // max character capacity per visual page representation

        if (rawBody.length <= textLimit) {
            pages.add(
                BookPage(
                    noteId = note.id,
                    dateStr = dateStr,
                    title = note.title,
                    bodySection = rawBody,
                    location = note.locationLabel,
                    mood = note.mood,
                    isContinuation = false
                )
            )
        } else {
            // Split entries across visual book folds safely
            var cursorStartIndex = 0
            var leafPartSequence = 1
            while (cursorStartIndex < rawBody.length) {
                val candidateEndIndex = (cursorStartIndex + textLimit).coerceAtMost(rawBody.length)
                var actualSplitIndex = candidateEndIndex

                // Walk back slightly to split on elegant whitespace rather than clipping words
                if (candidateEndIndex < rawBody.length) {
                    val matchingSpace = rawBody.substring(cursorStartIndex, candidateEndIndex).lastIndexOf(' ')
                    if (matchingSpace > textLimit / 2) {
                        actualSplitIndex = cursorStartIndex + matchingSpace
                    }
                }

                val bodySegment = rawBody.substring(cursorStartIndex, actualSplitIndex).trim()
                pages.add(
                    BookPage(
                        noteId = note.id,
                        dateStr = dateStr,
                        title = if (leafPartSequence == 1) note.title else "${note.title} (Part $leafPartSequence)",
                        bodySection = bodySegment + (if (actualSplitIndex < rawBody.length) "..." else ""),
                        location = note.locationLabel,
                        mood = note.mood,
                        isContinuation = leafPartSequence > 1
                    )
                )
                cursorStartIndex = actualSplitIndex
                leafPartSequence++
            }
        }
    }
    return pages
}
