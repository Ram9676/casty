package com.casty.music.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

class CastyHaptics internal constructor(
    private val view: View,
) {
    fun tick() {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun scrub() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun confirm() {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(effect)
    }

    fun gestureStart() {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_START
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(effect)
    }

    fun gestureEnd() {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_END
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(effect)
    }

    fun dragTick() {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun segmentTick() {
        if (Build.VERSION.SDK_INT >= 34) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun reject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun longPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun toggle(on: Boolean) {
        if (Build.VERSION.SDK_INT >= 34) {
            view.performHapticFeedback(
                if (on) HapticFeedbackConstants.TOGGLE_ON
                else HapticFeedbackConstants.TOGGLE_OFF
            )
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    // ===== PREMIUM FUTURE HAPTICS (God-level delightful feel) =====

    /** Strong, satisfying impact for primary actions (play, confirm big actions) */
    fun heavyImpact() {
        if (Build.VERSION.SDK_INT >= 34) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /** Light, crisp tick for fine adjustments (volume, small chips) */
    fun lightTick() {
        if (Build.VERSION.SDK_INT >= 34) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /** Success / positive feedback (like, added to queue, save) */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        // Subtle double tap feel on newer devices
        if (Build.VERSION.SDK_INT >= 34) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /** Error / reject with texture */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /** Premium slider/scrub texture — call repeatedly while dragging for "physical" feel */
    fun textureScrub(intensity: Float = 1f) {
        val effect = if (Build.VERSION.SDK_INT >= 34) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(effect)
    }

    /** Big delightful press for hero elements (album art, main play button) */
    fun heroPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (Build.VERSION.SDK_INT >= 34) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        }
    }
}

@Composable
fun rememberCastyHaptics(): CastyHaptics {
    val view = LocalView.current
    return remember(view) { CastyHaptics(view) }
}
