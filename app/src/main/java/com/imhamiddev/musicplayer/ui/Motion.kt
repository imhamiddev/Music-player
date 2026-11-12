package com.imhamiddev.musicplayer.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * Single source of truth for every animation in the app.
 * Everything is a slow, critically-damped ease (no bounce, no overshoot) so
 * opening, closing and changing things always feels calm and silky.
 */
object Motion {
    /** Gentle ease-in-out: starts softly, glides, lands softly. */
    val Silk = CubicBezierEasing(0.37f, 0f, 0.15f, 1f)

    const val Press = 320        // tap feedback
    const val Color = 600        // colour / tint changes
    const val Short = 650        // small elements appearing
    const val Medium = 850       // content swaps, tab slides
    const val Long = 1000        // full-screen panels

    fun <T> soft(ms: Int = Medium): FiniteAnimationSpec<T> = tween(ms, easing = Silk)
}
