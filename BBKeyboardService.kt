package com.bbexperience.launcher

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
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
    private val predictionEngine = BBPredictionEngine()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        vibrator = BBVibrator(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
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
                    getCurrentWord = { getCurrentComposingWord() },
                    onCommitText = { text ->
                        vibrator.keyClick()
                        currentInputConnection?.commitText(text, 1)
                    },
                    onDelete = {
                        vibrator.keyClick()
                        handleDelete()
                    },
                    onSpace = {
                        vibrator.keyClick()
                        currentInputConnection?.commitText(" ", 1)
                    },
                    onEnter = {
                        vibrator.keyClick()
                        handleEnter()
                    },
                    onSwipeLeftDeleteWord = {
                        vibrator.keyClick()
                        deleteLastWord()
                    },
                    onCursorMove = { delta ->
                        vibrator.trackpadTick()
                        moveCursor(delta)
                    },
                    onApplyPrediction = { word ->
                        vibrator.keyClick()
                        replaceCurrentWord(word)
                    }
                )
            }
        }
    }

    private fun getCurrentComposingWord(): String {
        val textBefore = currentInputConnection?.getTextBeforeCursor(30, 0)?.toString() ?: ""
        val lastSeparator = textBefore.lastIndexOfAny(charArrayOf(' ', '\n', '\t', ',', '.', ';', ':', '!', '?'))
        return if (lastSeparator == -1) textBefore else textBefore.substring(lastSeparator + 1)
    }

    private fun replaceCurrentWord(newWord: String) {
        val ic = currentInputConnection ?: return
        val current = getCurrentComposingWord()
        ic.deleteSurroundingText(current.length, 0)
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
        val options = currentInputEditorInfo.imeOptions
        val action = options and android.view.inputmethod.EditorInfo.IME_MASK_ACTION
        if (action != 0 && (options and android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
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
