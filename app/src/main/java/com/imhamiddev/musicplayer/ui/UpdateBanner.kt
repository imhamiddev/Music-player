package com.imhamiddev.musicplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.UpdateState

/** Height of the banner's content row (below the status bar). The library is pushed down by this much. */
val UpdateBannerHeight: Dp = 40.dp

/** Slim banner at the very top: new version found → Download → progress → Install. Never auto-updates. */
@Composable
fun UpdateBanner(
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val str = LocalStrings.current
    val held = rememberHeld(state.takeIf { it != UpdateState.None })
    AnimatedVisibility(
        visible = state != UpdateState.None,
        modifier = modifier,
        enter = slideInVertically(Motion.soft(Motion.Long)) { -it } + fadeIn(Motion.soft(Motion.Long)),
        exit = slideOutVertically(Motion.soft(Motion.Long)) { -it } + fadeOut(Motion.soft(Motion.Long)),
    ) {
        Box(Modifier.fillMaxWidth().background(Ui.GreenBrush)) {
            Column(Modifier.statusBarsPadding()) {
                Crossfade(held, animationSpec = Motion.soft(Motion.Short), label = "bannerState") { s ->
                    Row(
                        Modifier.fillMaxWidth().height(UpdateBannerHeight).padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, null, tint = Ui.OnAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            when (s) {
                                is UpdateState.Available -> str.updateAvailable(s.info.version)
                                is UpdateState.Downloading -> str.updateDownloading((s.progress * 100).toInt())
                                is UpdateState.Ready -> str.updateReady(s.info.version)
                                is UpdateState.Failed -> str.updateFailed
                                else -> ""
                            },
                            color = Ui.OnAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        val action: Pair<String, () -> Unit>? = when (s) {
                            is UpdateState.Available -> str.download to onDownload
                            is UpdateState.Ready -> str.install to onInstall
                            is UpdateState.Failed -> str.retry to onDownload
                            else -> null
                        }
                        if (action != null) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Ui.OnAccent)
                                    .bouncyClick(0.94f, onClick = action.second)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(action.first, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (s !is UpdateState.Downloading) {
                            Box(
                                Modifier.size(36.dp).bouncyClick(0.85f, onClick = onDismiss),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Close, "Dismiss", tint = Ui.OnAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                val p = (held as? UpdateState.Downloading)?.progress ?: 0f
                val shown by animateFloatAsState(p, tween(400, easing = LinearEasing), label = "dl")
                Box(Modifier.fillMaxWidth().height(3.dp)) {
                    if (held is UpdateState.Downloading) {
                        Box(Modifier.fillMaxWidth(shown).height(3.dp).background(Ui.OnAccent.copy(alpha = 0.65f)))
                    }
                }
            }
        }
    }
}
