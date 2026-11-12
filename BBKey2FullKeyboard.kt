package com.bbexperience.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BBKey2FullKeyboard(
    vibrator: BBVibrator,
    predictionEngine: BBPredictionEngine,
    getCurrentWord: () -> String,
    onCommitText: (String) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onSwipeLeftDeleteWord: () -> Unit,
    onCursorMove: (Int) -> Unit,
    onApplyPrediction: (String) -> Unit,
    onShortcutTriggered: (String) -> Unit = {}
) {
    var isAltActive by remember { mutableStateOf(false) }
    var isShiftActive by remember { mutableStateOf(false) }
    var isSpeedKeyActive by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }
    var activeWord by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf(listOf<String>()) }
    var isSpaceScrubbing by remember { mutableStateOf(false) }
    var scrubAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val word = getCurrentWord()
            if (word != activeWord) {
                activeWord = word
                predictions = predictionEngine.getPredictions(word)
            }
            delay(100)
        }
    }

    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")

    val altMap = mapOf(
        "Q" to "#", "W" to "1", "E" to "2", "R" to "3", "T" to "(",
        "Y" to ")", "U" to "_", "I" to "-", "O" to "+", "P" to "@",
        "A" to "*", "S" to "4", "D" to "5", "F" to "6", "G" to "/",
        "H" to ":", "J" to ";", "K" to "'", "L" to "\"",
        "Z" to "7", "X" to "8", "C" to "9", "V" to "?", "B" to "!",
        "N" to ",", "M" to "."
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0D0F))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Color(0xFF14161A)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable {
                        vibrator.keyClick()
                        showEmojiSheet = !showEmojiSheet
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (showEmojiSheet) "ABC" else "😀", fontSize = 16.sp, color = Color.White)
            }

            predictions.take(3).forEach { cand ->
                var dragY by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(cand) {
                            detectDragGestures(
                                onDrag = { _, delta -> dragY += delta.y },
                                onDragEnd = {
                                    if (dragY < -20f) {
                                        onApplyPrediction(cand)
                                    }
                                    dragY = 0f
                                }
                            )
                        }
                        .clickable { onApplyPrediction(cand) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("▲", fontSize = 7.sp, color = Color(0xFFD4AF37))
                        Text(cand, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (showEmojiSheet) {
            BBEmojiMatrix(
                onSelectEmoji = { emoji -> onCommitText(emoji) },
                onClose = {
                    vibrator.keyClick()
                    showEmojiSheet = false
                }
            )
        } else {
            FretBar()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row1.forEach { char ->
                    val keyChar = resolveChar(char, isAltActive, isShiftActive, altMap)
                    BBKeyCap(
                        char = keyChar,
                        subChar = altMap[char],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isSpeedKeyActive) {
                                vibrator.keyClick()
                                onShortcutTriggered(char)
                                isSpeedKeyActive = false
                            } else {
                                onCommitText(keyChar)
                                isAltActive = false
                            }
                        }
                    )
                }
            }

            FretBar()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row2.forEach { char ->
                    val keyChar = resolveChar(char, isAltActive, isShiftActive, altMap)
                    BBKeyCap(
                        char = keyChar,
                        subChar = altMap[char],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isSpeedKeyActive) {
                                vibrator.keyClick()
                                onShortcutTriggered(char)
                                isSpeedKeyActive = false
                            } else {
                                onCommitText(keyChar)
                                isAltActive = false
                            }
                        }
                    )
                }
            }

            FretBar()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BBKeyCap(
                    char = "⇧",
                    modifier = Modifier.weight(1.25f),
                    isSpecial = true,
                    isActive = isShiftActive,
                    onClick = {
                        vibrator.keyClick()
                        isShiftActive = !isShiftActive
                    }
                )

                row3.forEach { char ->
                    val keyChar = resolveChar(char, isAltActive, isShiftActive, altMap)
                    BBKeyCap(
                        char = keyChar,
                        subChar = altMap[char],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isSpeedKeyActive) {
                                vibrator.keyClick()
                                onShortcutTriggered(char)
                                isSpeedKeyActive = false
                            } else {
                                onCommitText(keyChar)
                                isAltActive = false
                            }
                        }
                    )
                }

                BBKeyCap(
                    char = "⌫",
                    modifier = Modifier.weight(1.25f),
                    isSpecial = true,
                    onClick = { onDelete() },
                    onSwipeLeft = { onSwipeLeftDeleteWord() }
                )
            }

            FretBar()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BBKeyCap(
                    char = "alt",
                    modifier = Modifier.weight(1.2f),
                    isSpecial = true,
                    isActive = isAltActive,
                    onClick = {
                        vibrator.keyClick()
                        isAltActive = !isAltActive
                    }
                )

                BBKeyCap(
                    char = if (isAltActive) "$" else "0",
                    subChar = if (!isAltActive) "$" else null,
                    modifier = Modifier.weight(1.1f),
                    onClick = {
                        onCommitText(if (isAltActive) "$" else "0")
                        isAltActive = false
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(4.4f)
                        .height(46.dp)
                        .border(0.5.dp, Color(0xFF383A40), RoundedCornerShape(3.dp))
                        .background(
                            if (isSpaceScrubbing) Color(0xFF2C3E50) else Color(0xFF1F2125),
                            RoundedCornerShape(3.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    val released = tryAwaitRelease()
                                    if (isSpaceScrubbing) {
                                        isSpaceScrubbing = false
                                    } else if (released) {
                                        onSpace()
                                    }
                                },
                                onLongPress = {
                                    isSpaceScrubbing = true
                                    vibrator.trackpadTick()
                                }
                            )
                        }
                        .pointerInput(isSpaceScrubbing) {
                            if (isSpaceScrubbing) {
                                detectDragGestures { _, dragAmount ->
                                    scrubAccumulator += dragAmount.x
                                    if (scrubAccumulator > 25f) {
                                        onCursorMove(1)
                                        scrubAccumulator = 0f
                                    } else if (scrubAccumulator < -25f) {
                                        onCursorMove(-1)
                                        scrubAccumulator = 0f
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSpaceScrubbing) "◀  TRACKPAD CURSOR  ▶" else "BLACKBERRY",
                        color = if (isSpaceScrubbing) Color(0xFF70B0FF) else Color(0xFF555860),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                BBKeyCap(
                    char = ":::",
                    modifier = Modifier.weight(1.1f),
                    isSpecial = true,
                    isActive = isSpeedKeyActive,
                    onClick = {
                        isSpeedKeyActive = !isSpeedKeyActive
                        vibrator.keyClick()
                    }
                )

                BBKeyCap(
                    char = "↵",
                    modifier = Modifier.weight(1.3f),
                    isSpecial = true,
                    onClick = { onEnter() }
                )
            }
        }
    }
}

private fun resolveChar(char: String, isAlt: Boolean, isShift: Boolean, altMap: Map<String, String>): String {
    return when {
        isAlt && altMap.containsKey(char) -> altMap[char]!!
        isShift -> char.uppercase()
        else -> char.lowercase()
    }
}

@Composable
fun FretBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.2.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1E2024), Color(0xFF727680), Color(0xFF1E2024))
                )
            )
    )
}

@Composable
fun BBKeyCap(
    char: String,
    subChar: String? = null,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit,
    onSwipeLeft: (() -> Unit)? = null
) {
    var dragX by remember { mutableFloatStateOf(0f) }

    val baseColor = when {
        isActive -> Color(0xFF2E4057)
        isSpecial -> Color(0xFF181A1E)
        else -> Color(0xFF222429)
    }

    Box(
        modifier = modifier
            .height(46.dp)
            .border(0.6.dp, Color(0xFF383A42), RoundedCornerShape(3.dp))
            .background(baseColor, RoundedCornerShape(3.dp))
            .drawBehind {
                drawLine(
                    color = Color(0xFF484B55),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.6f
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, delta -> dragX += delta.x },
                    onDragEnd = {
                        if (dragX < -40f && onSwipeLeft != null) {
                            onSwipeLeft()
                        }
                        dragX = 0f
                    }
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (subChar != null) {
                Text(
                    text = subChar,
                    color = Color(0xFFD4AF37),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = char,
                color = if (isActive) Color(0xFF70B0FF) else Color(0xFFF0F2F5),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BBEmojiMatrix(onSelectEmoji: (String) -> Unit, onClose: () -> Unit) {
    val emojis = listOf(
        "😀", "😃", "😄", "😁", "😅", "😂", "🤣", "😊", "😇", "🙂",
        "😉", "😌", "😍", "🥰", "😘", "😎", "🥳", "😏", "😒", "😞",
        "😔", "😟", "😕", "🙁", "😣", "😖", "😫", "😩", "🥺", "😢",
        "😭", "😤", "😠", "😡", "🤯", "😳", "🥵", "🥶", "😱", "😨",
        "👍", "👎", "👌", "✌️", "🤞", "🤝", "👏", "🙌", "🔥", "✨",
        "🎉", "⭐", "❤️", "💼", "✉️", "📅", "📈", "📌", "✅", "❌"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF14161A))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SYMBOLS & EMOJI", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Back to ABC", color = Color(0xFF70B0FF), fontSize = 11.sp, modifier = Modifier.clickable { onClose() })
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onSelectEmoji(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
            }
        }
    }
}
