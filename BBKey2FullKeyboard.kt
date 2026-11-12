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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BBKey2FullKeyboard(
    vibrator: BBVibrator,
    predictionEngine: BBPredictionEngine,
    keyboardScale: Float = 1.0f,
    onScaleChange: ((Float) -> Unit)? = null,
    currentWordContext: String = "",
    previousWordContext: String = "",
    getCurrentWord: (() -> String)? = null,
    getPreviousWord: (() -> String)? = null,
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
    var isSpaceScrubbing by remember { mutableStateOf(false) }
    var scrubAccumulator by remember { mutableFloatStateOf(0f) }

    var localWordBuffer by remember { mutableStateOf("") }
    var localPrevWord by remember { mutableStateOf("") }
    var predictions by remember {
        mutableStateOf(predictionEngine.getPredictions(currentWordContext, previousWordContext))
    }

    var deckSize by remember { mutableStateOf(IntSize(1, 1)) }
    val flowTrail = remember { mutableStateListOf<Char>() }

    val keyHeight: Dp = (44f * keyboardScale).coerceIn(32f, 60f).dp
    val fontSizeSp = (13f * keyboardScale).coerceIn(10f, 17f).sp

    fun refreshSuggestions(overrideCurrent: String? = null, overridePrev: String? = null) {
        val active = overrideCurrent ?: getCurrentWord?.invoke() ?: currentWordContext.ifEmpty { localWordBuffer }
        val prev = overridePrev ?: getPreviousWord?.invoke() ?: previousWordContext.ifEmpty { localPrevWord }
        predictions = predictionEngine.getPredictions(active, prev)
    }

    LaunchedEffect(currentWordContext, previousWordContext) {
        if (flowTrail.isEmpty()) {
            refreshSuggestions(currentWordContext, previousWordContext)
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

    fun resolveKeyAtPosition(pos: Offset): Char? {
        if (deckSize.width <= 1 || deckSize.height <= 1) return null
        val normX = (pos.x / deckSize.width.toFloat()).coerceIn(0f, 0.999f)
        val normY = (pos.y / deckSize.height.toFloat()).coerceIn(0f, 0.999f)
        return when {
            normY < 0.34f -> row1[(normX * 10).toInt().coerceIn(0, 9)].first()
            normY < 0.67f -> row2[(normX * 9).toInt().coerceIn(0, 8)].first()
            else -> {
                val idx = ((normX - 0.12f) / 0.76f * 7).toInt()
                if (idx in 0..6) row3[idx].first() else null
            }
        }
    }

    fun handleCharacterTyped(charStr: String) {
        onCommitText(charStr)
        if (charStr.length == 1 && charStr[0].isLetter()) {
            localWordBuffer += charStr
        } else {
            if (localWordBuffer.isNotEmpty()) localPrevWord = localWordBuffer
            localWordBuffer = ""
        }
        refreshSuggestions(localWordBuffer, localPrevWord)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0D0F))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        if (onScaleChange != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .pointerInput(keyboardScale) {
                        detectDragGestures { _, dragAmount ->
                            val deltaScale = -dragAmount.y / 300f
                            onScaleChange((keyboardScale + deltaScale).coerceIn(0.75f, 1.35f))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .background(Color(0xFF3E424B), RoundedCornerShape(2.dp))
                )
            }
        }

        // GBOARD-STYLE LIVE SUGGESTION BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Color(0xFF14161A)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .clickable {
                        vibrator.keyClick()
                        showEmojiSheet = !showEmojiSheet
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (showEmojiSheet) "ABC" else "😀", fontSize = 15.sp, color = Color.White)
            }

            if (flowTrail.isNotEmpty() && predictions.isEmpty()) {
                Text(
                    text = "Flow: ${flowTrail.joinToString("")}",
                    color = Color(0xFFD4AF37),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                predictions.take(3).forEachIndexed { index, cand ->
                    var dragY by remember { mutableFloatStateOf(0f) }
                    val isTopMatch = index == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp, vertical = 3.dp)
                            .background(
                                if (isTopMatch) Color(0xFF212630) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .pointerInput(cand) {
                                detectDragGestures(
                                    onDrag = { _, delta -> dragY += delta.y },
                                    onDragEnd = {
                                        if (dragY < -16f) {
                                            onApplyPrediction(cand)
                                            localPrevWord = cand
                                            localWordBuffer = ""
                                            refreshSuggestions("", cand)
                                        }
                                        dragY = 0f
                                    }
                                )
                            }
                            .clickable {
                                onApplyPrediction(cand)
                                localPrevWord = cand
                                localWordBuffer = ""
                                refreshSuggestions("", cand)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("▲", fontSize = 6.sp, color = if (isTopMatch) Color(0xFFD4AF37) else Color(0xFF6B7280))
                            Text(
                                text = cand,
                                color = if (isTopMatch) Color.White else Color(0xFFD1D5DB),
                                fontSize = 13.sp,
                                fontWeight = if (isTopMatch) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (showEmojiSheet) {
            BBEmojiMatrix(
                onSelectEmoji = { emoji -> handleCharacterTyped(emoji) },
                onClose = {
                    vibrator.keyClick()
                    showEmojiSheet = false
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { deckSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                flowTrail.clear()
                                resolveKeyAtPosition(startOffset)?.let {
                                    flowTrail.add(it)
                                    vibrator.trackpadTick()
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val keyChar = resolveKeyAtPosition(change.position)
                                if (keyChar != null && flowTrail.lastOrNull() != keyChar) {
                                    flowTrail.add(keyChar)
                                    vibrator.trackpadTick()
                                    predictions = predictionEngine.matchFlowGesture(flowTrail)
                                }
                            },
                            onDragEnd = {
                                if (flowTrail.size >= 2) {
                                    val matches = predictionEngine.matchFlowGesture(flowTrail)
                                    val bestWord = matches.firstOrNull()
                                    if (bestWord != null) {
                                        val formatted = if (isShiftActive) {
                                            bestWord.replaceFirstChar { it.uppercase() }
                                        } else bestWord
                                        onCommitText("$formatted ")
                                        localPrevWord = formatted
                                        localWordBuffer = ""
                                        predictions = predictionEngine.getPredictions("", formatted)
                                        isShiftActive = false
                                    }
                                }
                                flowTrail.clear()
                            },
                            onDragCancel = { flowTrail.clear() }
                        )
                    }
            ) {
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
                            keyHeight = keyHeight,
                            fontSize = fontSizeSp,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isSpeedKeyActive) {
                                    vibrator.keyClick()
                                    onShortcutTriggered(char)
                                    isSpeedKeyActive = false
                                } else {
                                    handleCharacterTyped(keyChar)
                                    isAltActive = false
                                    if (isShiftActive) isShiftActive = false
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
                            keyHeight = keyHeight,
                            fontSize = fontSizeSp,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isSpeedKeyActive) {
                                    vibrator.keyClick()
                                    onShortcutTriggered(char)
                                    isSpeedKeyActive = false
                                } else {
                                    handleCharacterTyped(keyChar)
                                    isAltActive = false
                                    if (isShiftActive) isShiftActive = false
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
                        keyHeight = keyHeight,
                        fontSize = fontSizeSp,
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
                            keyHeight = keyHeight,
                            fontSize = fontSizeSp,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isSpeedKeyActive) {
                                    vibrator.keyClick()
                                    onShortcutTriggered(char)
                                    isSpeedKeyActive = false
                                } else {
                                    handleCharacterTyped(keyChar)
                                    isAltActive = false
                                    if (isShiftActive) isShiftActive = false
                                }
                            }
                        )
                    }

                    BBKeyCap(
                        char = "⌫",
                        keyHeight = keyHeight,
                        fontSize = fontSizeSp,
                        modifier = Modifier.weight(1.25f),
                        isSpecial = true,
                        onClick = {
                            onDelete()
                            if (localWordBuffer.isNotEmpty()) {
                                localWordBuffer = localWordBuffer.dropLast(1)
                            }
                            refreshSuggestions(localWordBuffer, localPrevWord)
                        },
                        onSwipeLeft = {
                            onSwipeLeftDeleteWord()
                            localWordBuffer = ""
                            refreshSuggestions("", localPrevWord)
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
                    char = "alt",
                    keyHeight = keyHeight,
                    fontSize = fontSizeSp,
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
                    keyHeight = keyHeight,
                    fontSize = fontSizeSp,
                    modifier = Modifier.weight(1.1f),
                    onClick = {
                        handleCharacterTyped(if (isAltActive) "$" else "0")
                        isAltActive = false
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(4.4f)
                        .height(keyHeight)
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
                                        if (localWordBuffer.isNotEmpty()) localPrevWord = localWordBuffer
                                        localWordBuffer = ""
                                        refreshSuggestions("", localPrevWord)
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
                    keyHeight = keyHeight,
                    fontSize = fontSizeSp,
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
                    keyHeight = keyHeight,
                    fontSize = fontSizeSp,
                    modifier = Modifier.weight(1.3f),
                    isSpecial = true,
                    onClick = {
                        onEnter()
                        localWordBuffer = ""
                        localPrevWord = ""
                        refreshSuggestions("", "")
                    }
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
    keyHeight: Dp = 44.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
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
            .height(keyHeight)
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
                fontSize = fontSize,
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
            .height(190.dp)
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
