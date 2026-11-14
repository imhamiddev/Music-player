@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.imhamiddev.musicplayer.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Shared-element helpers for the mini player <-> full player transition. The scopes are provided by
 * [MusicApp]; when they are missing (other screens that reuse these pieces) every helper is a no-op.
 */
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** A soft spring with a tiny overshoot: the shapes settle instead of stopping. */
private val morphSpring = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.84f, stiffness = 300f, visibilityThreshold = Rect.VisibilityThreshold)
}

/** Clip whose corner radius follows the size of the morphing bounds (small -> big). */
private fun morphClip(smallSize: Dp, bigSize: Dp, smallRadius: Dp, bigRadius: Dp, byHeight: Boolean) =
    object : SharedTransitionScope.OverlayClip {
        override fun getClipPath(
            sharedContentState: SharedTransitionScope.SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Path {
            val s = with(density) { smallSize.toPx() }
            val b = with(density) { bigSize.toPx() }
            val cur = if (byHeight) bounds.height else bounds.width
            val f = ((cur - s) / (b - s)).coerceIn(0f, 1f)
            val r = with(density) { smallRadius.toPx() * (1f - f) + bigRadius.toPx() * f }
            return Path().apply { addRoundRect(RoundRect(bounds, CornerRadius(r, r))) }
        }
    }

/** The dark deck: 26dp-rounded mini bar growing into the 36dp-rounded deck of the full player. */
@Composable
fun rememberDeckClip(): SharedTransitionScope.OverlayClip =
    remember { morphClip(88.dp, 200.dp, 26.dp, 36.dp, byHeight = true) }

/** The artwork: 42dp / 14dp square growing into the big 40dp-rounded cover. */
@Composable
fun rememberArtClip(): SharedTransitionScope.OverlayClip {
    val big = (LocalConfiguration.current.screenWidthDp - 56).dp
    return remember(big) { morphClip(42.dp, big, 14.dp, 40.dp, byHeight = false) }
}

/** Same bounds on both screens: the box moves and resizes, contents cross-fade (container transform). */
@Composable
fun Modifier.morphBounds(key: Any, clip: SharedTransitionScope.OverlayClip? = null): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalAnimScope.current ?: return this
    return with(shared) {
        val state = rememberSharedContentState(key)
        val enter = fadeIn(tween(240, easing = Motion.Silk))
        val exit = fadeOut(tween(180, easing = Motion.Silk))
        val resize = SharedTransitionScope.ResizeMode.ScaleToBounds(ContentScale.FillWidth)
        if (clip != null) {
            this@morphBounds.sharedBounds(
                sharedContentState = state, animatedVisibilityScope = anim,
                enter = enter, exit = exit, boundsTransform = morphSpring, resizeMode = resize,
                clipInOverlayDuringTransition = clip,
            )
        } else {
            this@morphBounds.sharedBounds(
                sharedContentState = state, animatedVisibilityScope = anim,
                enter = enter, exit = exit, boundsTransform = morphSpring, resizeMode = resize,
            )
        }
    }
}

/** The very same picture on both screens: it flies from one place to the other. */
@Composable
fun Modifier.morphElement(key: Any, clip: SharedTransitionScope.OverlayClip? = null): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalAnimScope.current ?: return this
    return with(shared) {
        val state = rememberSharedContentState(key)
        if (clip != null) {
            this@morphElement.sharedElement(
                state = state, animatedVisibilityScope = anim,
                boundsTransform = morphSpring, clipInOverlayDuringTransition = clip,
            )
        } else {
            this@morphElement.sharedElement(
                state = state, animatedVisibilityScope = anim, boundsTransform = morphSpring,
            )
        }
    }
}

/** Fades a piece of the full player in a moment after the container, sliding a little from [fromY] px. */
@Composable
fun Modifier.staggerIn(delayMs: Int, fromY: Int): Modifier {
    val anim = LocalAnimScope.current ?: return this
    return with(anim) {
        this@staggerIn.animateEnterExit(
            enter = fadeIn(tween(340, delayMillis = delayMs, easing = Motion.Silk)) +
                slideInVertically(tween(420, delayMillis = delayMs, easing = Motion.Silk)) { fromY },
            exit = fadeOut(tween(140, easing = Motion.Silk)),
        )
    }
}
