package com.imhamiddev.musicplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** Keeps the last non-null value so a dialog can finish its exit animation after its target is cleared. */
@Composable
fun <T : Any> rememberHeld(value: T?): T? {
    val holder = remember { arrayOfNulls<Any>(1) }
    if (value != null) holder[0] = value
    @Suppress("UNCHECKED_CAST")
    return holder[0] as T?
}

private class BelowAnchor(private val gapPx: Int, private val padPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.right - popupContentSize.width + padPx)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        var y = anchorBounds.bottom + gapPx - padPx
        if (y + popupContentSize.height > windowSize.height) {
            y = (anchorBounds.top - gapPx - popupContentSize.height + padPx).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}

/** Drop-down menu that opens and closes with a slow, silky fade + scale (Material's own is too snappy). */
@Composable
fun SoftMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    shape: Shape,
    gap: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = expanded
    if (!state.currentState && !state.targetState) return
    val pad = 20.dp
    val density = LocalDensity.current
    val provider = remember(density) { BelowAnchor(with(density) { gap.roundToPx() }, with(density) { pad.roundToPx() }) }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn(Motion.soft(Motion.Short)) + scaleIn(Motion.soft(Motion.Short), initialScale = 0.88f, transformOrigin = TransformOrigin(1f, 0f)),
            exit = fadeOut(Motion.soft(Motion.Short)) + scaleOut(Motion.soft(Motion.Short), targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 0f)),
        ) {
            Column(
                Modifier
                    .padding(pad)
                    .shadow(18.dp, shape, ambientColor = Color.Black.copy(0.12f), spotColor = Color.Black.copy(0.2f))
                    .clip(shape)
                    .background(Ui.Card),
                content = content,
            )
        }
    }
}

/** Alert dialog with a slow fade/scale and a softly fading scrim. Same shape as Material's AlertDialog. */
@Composable
fun SoftAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    visible: Boolean = true,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(0f)
            window?.setWindowAnimations(-1)
        }
        val none = remember { MutableInteractionSource() }
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = none, indication = null) { onDismissRequest() },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visibleState = state,
                enter = fadeIn(Motion.soft(Motion.Medium)),
                exit = fadeOut(Motion.soft(Motion.Medium)),
            ) { Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f))) }
            AnimatedVisibility(
                visibleState = state,
                enter = fadeIn(Motion.soft(Motion.Medium)) + scaleIn(Motion.soft(Motion.Medium), initialScale = 0.92f),
                exit = fadeOut(Motion.soft(Motion.Medium)) + scaleOut(Motion.soft(Motion.Medium), targetScale = 0.94f),
            ) {
                Column(
                    Modifier
                        .padding(horizontal = 28.dp)
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Ui.Card)
                        .clickable(interactionSource = none, indication = null) { }
                        .padding(24.dp),
                ) {
                    title()
                    Spacer(Modifier.height(14.dp))
                    text()
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    }
}
