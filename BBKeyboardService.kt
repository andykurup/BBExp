package com.bbexperience.launcher

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class BBKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private lateinit var vibrator: BBVibrator
    private lateinit var prefs: BBPreferences
    private val predictionEngine = BBPredictionEngine()

    private var activeWordState by mutableStateOf("")
    private var previousWordState by mutableStateOf("")

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        vibrator = BBVibrator(this)
        prefs = BBPreferences(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        syncCursorWords()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        syncCursorWords()
    }

    private fun syncCursorWords() {
        val before = currentInputConnection?.getTextBeforeCursor(50, 0)?.toString() ?: ""
        val separators = charArrayOf(' ', '\n', '\t', ',', '.', ';', ':', '!', '?')
        val lastSep = before.lastIndexOfAny(separators)
        val curr = if (lastSep == -1) before else before.substring(lastSep + 1)
        val priorText = if (lastSep <= 0) "" else before.substring(0, lastSep).trimEnd()
        val prevSep = priorText.lastIndexOfAny(separators)
        val prev = if (prevSep == -1) priorText else priorText.substring(prevSep + 1)

        activeWordState = curr
        previousWordState = prev
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BBKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@BBKeyboardService)

            setContent {
                BBKey2FullKeyboard(
                    vibrator = vibrator,
                    predictionEngine = predictionEngine,
                    keyboardScale = prefs.keyboardScale,
                    currentWordContext = activeWordState,
                    previousWordContext = previousWordState,
                    getCurrentWord = { activeWordState },
                    getPreviousWord = { previousWordState },
                    onCommitText = { text ->
                        vibrator.keyClick()
                        currentInputConnection?.commitText(text, 1)
                        syncCursorWords()
                    },
                    onDelete = {
                        vibrator.keyClick()
                        handleDelete()
                        syncCursorWords()
                    },
                    onSpace = {
                        vibrator.keyClick()
                        currentInputConnection?.commitText(" ", 1)
                        syncCursorWords()
                    },
                    onEnter = {
                        vibrator.keyClick()
                        handleEnter()
                        syncCursorWords()
                    },
                    onSwipeLeftDeleteWord = {
                        vibrator.keyClick()
                        deleteLastWord()
                        syncCursorWords()
                    },
                    onCursorMove = { delta ->
                        vibrator.trackpadTick()
                        moveCursor(delta)
                    },
                    onApplyPrediction = { word ->
                        vibrator.keyClick()
                        replaceCurrentWord(word)
                        syncCursorWords()
                    }
                )
            }
        }
    }

    private fun replaceCurrentWord(newWord: String) {
        val ic = currentInputConnection ?: return
        syncCursorWords()
        if (activeWordState.isNotEmpty()) {
            ic.deleteSurroundingText(activeWordState.length, 0)
        }
        ic.commitText("$newWord ", 1)
    }

    private fun moveCursor(delta: Int) {
        val ic = currentInputConnection ?: return
        if (delta > 0) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
        } else if (delta < 0) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
        }
    }

    private fun handleDelete() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (selected.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
    }

    private fun deleteLastWord() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
        if (before.isEmpty()) return
        val lastSpace = before.trimEnd().lastIndexOfAny(charArrayOf(' ', '\n', '\t'))
        val deleteCount = if (lastSpace == -1) before.length else before.length - (lastSpace + 1)
        ic.deleteSurroundingText(deleteCount, 0)
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        val options = currentInputEditorInfo?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        if (action != 0 && (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            ic.performEditorAction(action)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
