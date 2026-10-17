package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.presentation.ui.MorphShape
import com.supernova.networkswitch.presentation.ui.polygon
import com.supernova.networkswitch.presentation.viewmodel.SwitchEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroModeCard(
    currentMode: NetworkMode?,
    nextMode: NetworkMode,
    isOnMode: Boolean,
    switchingTo: NetworkMode?,
    events: Flow<SwitchEvent>,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme
    val haptics = LocalHapticFeedback.current
    val switching = switchingTo != null

    // Shape morph: remember where the last morph ended and animate to the new polygon.
    val target = currentMode.polygon()
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val morphProgress = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val shakeX = remember { Animatable(0f) }

    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from = to
        to = target
        morphProgress.snapTo(0f)
        launch { rotation.animateTo(rotation.value + 90f, motion.slowSpatialSpec()) }
        morphProgress.animateTo(1f, motion.slowSpatialSpec())
    }
    // Spin while the radio works, then settle on the nearest quarter turn.
    LaunchedEffect(switching) {
        if (switching) {
            while (true) rotation.animateTo(rotation.value + 360f, tween(1400, easing = LinearEasing))
        } else {
            rotation.animateTo(ceil(rotation.value / 90f) * 90f, motion.defaultSpatialSpec())
        }
    }
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is SwitchEvent.Switched -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    pop.animateTo(1.08f, motion.fastSpatialSpec())
                    pop.animateTo(1f, motion.slowSpatialSpec())
                }
                is SwitchEvent.Failed -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    shakeX.shake()
                }
            }
        }
    }

    val morph = remember(from, to) { Morph(from, to) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        pressScale.animateTo(if (pressed) 0.9f else 1f, motion.fastSpatialSpec())
    }

    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when {
            currentMode == null -> colors.surfaceContainerHighest
            isOnMode -> colors.primary
            else -> colors.tertiaryContainer
        },
        motion.slowEffectsSpec(),
        label = "heroContainer",
    )
    val content by animateColorAsState(
        when {
            currentMode == null -> colors.onSurfaceVariant
            isOnMode -> colors.onPrimary
            else -> colors.onTertiaryContainer
        },
        motion.slowEffectsSpec(),
        label = "heroContent",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shakeX.value * density },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(248.dp)
                .graphicsLayer {
                    val s = pop.value * pressScale.value
                    scaleX = s
                    scaleY = s
                }
                .clip(MorphShape(morph, morphProgress.value, rotation.value))
                .background(container)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = !switching,
                    role = Role.Button,
                    onClickLabel = "Switch to ${nextMode.title}",
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onToggle()
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = currentMode?.shortLabel ?: "?",
                transitionSpec = {
                    (slideInVertically(motion.defaultSpatialSpec()) { it / 2 } +
                        scaleIn(motion.defaultSpatialSpec(), initialScale = 0.6f) +
                        fadeIn(motion.defaultEffectsSpec())) togetherWith
                        (slideOutVertically(motion.fastSpatialSpec()) { -it / 2 } +
                            scaleOut(motion.fastSpatialSpec(), targetScale = 0.6f) +
                            fadeOut(motion.fastEffectsSpec())) using SizeTransform(clip = false)
                },
                label = "heroLabel",
            ) { label ->
                BasicText(
                    text = label,
                    modifier = Modifier.padding(horizontal = 44.dp),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = content,
                        letterSpacing = (-2).sp,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 104.sp),
                )
            }
        }

        Spacer(Modifier.size(20.dp))

        AnimatedContent(
            targetState = switchingTo?.let { "Switching to ${it.title}…" }
                ?: currentMode?.let { if (it.generations.size == 1) "${it.title} · ${it.technology}" else it.technology }
                ?: "Mode unknown",
            transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
            label = "heroCaption",
        ) { caption ->
            Text(
                caption,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.size(20.dp))

        val height = ButtonDefaults.MediumContainerHeight
        // Stays enabled while switching so the loading indicator keeps full colour;
        // the view model ignores taps until the switch finishes.
        Button(
            onClick = {
                if (switching) return@Button
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                onToggle()
            },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
        ) {
            AnimatedContent(
                targetState = switching,
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec())) togetherWith
                        (fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec()))
                },
                contentAlignment = Alignment.Center,
                label = "heroButton",
            ) { busy ->
                if (busy) {
                    LoadingIndicator(Modifier.size(32.dp), color = LocalContentColor.current)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(
                            Icons.Rounded.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
                        )
                        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                        Text("Switch to ${nextMode.shortLabel}", style = ButtonDefaults.textStyleFor(height))
                    }
                }
            }
        }
    }
}
