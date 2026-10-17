package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.presentation.ui.iconRes
import com.supernova.networkswitch.presentation.ui.polygon

/** A mode's glyph inside its signature shape. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModeBadge(
    mode: NetworkMode?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(mode.polygon().toShape())
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(mode.iconRes()), contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        action()
    }
}

/** Pinned modes as a connected button group; the active one stays checked. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun PinnedModesSection(
    pinned: List<NetworkMode>,
    currentMode: NetworkMode?,
    switchingTo: NetworkMode?,
    onApply: (NetworkMode) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier) {
        SectionHeader("Quick modes") {
            FilledTonalIconButton(onClick = onEdit, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit quick modes")
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            pinned.forEachIndexed { index, mode ->
                ToggleButton(
                    checked = mode == currentMode,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onApply(mode)
                    },
                    enabled = switchingTo == null,
                    shapes = when {
                        pinned.size == 1 -> ToggleButtonDefaults.shapesFor(56.dp)
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == pinned.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .semantics { role = Role.RadioButton },
                ) {
                    AnimatedContent(
                        targetState = switchingTo == mode,
                        transitionSpec = { (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut()) },
                        label = "pinBusy",
                    ) { busy ->
                        if (busy) {
                            LoadingIndicator(Modifier.size(24.dp), color = LocalContentColor.current)
                        } else {
                            Text(mode.shortLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** The two modes the tile flips between, with a spinning swap in the middle. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TilePairSection(
    modeA: NetworkMode,
    modeB: NetworkMode,
    currentMode: NetworkMode?,
    onPickA: () -> Unit,
    onPickB: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var turns by remember { mutableFloatStateOf(0f) }
    val spin by animateFloatAsState(turns * 180f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "swapSpin")
    val haptics = LocalHapticFeedback.current

    Column(modifier) {
        SectionHeader("Quick Settings tile")
        Row(verticalAlignment = Alignment.CenterVertically) {
            PairSlot(modeA, caption = "Tile off", active = currentMode == modeA, onClick = onPickA, modifier = Modifier.weight(1f))
            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    turns += 1
                    onSwap()
                },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap", modifier = Modifier.graphicsLayer { rotationZ = spin })
            }
            PairSlot(modeB, caption = "Tile on", active = currentMode == modeB, onClick = onPickB, modifier = Modifier.weight(1f))
        }
        Text(
            "Tap the tile to flip between these. Long-press it to open the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PairSlot(mode: NetworkMode, caption: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val effects = motion.defaultEffectsSpec<Color>()
    val container by animateColorAsState(if (active) colors.primaryContainer else colors.surfaceContainerHigh, effects, label = "slot")
    val onContainer by animateColorAsState(if (active) colors.onPrimaryContainer else colors.onSurface, effects, label = "slotContent")

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = onContainer,
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            ModeBadge(mode)
            Spacer(Modifier.size(12.dp))
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), 0.8f)) togetherWith
                        fadeOut(motion.fastEffectsSpec())
                },
                label = "slotMode",
            ) {
                Text(it.shortLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(caption, style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.72f))
        }
    }
}

/** Offers Android 13's one-tap "add tile" prompt until the tile is in place. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddTileBanner(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Switch from anywhere", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Add the tile to Quick Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.size(12.dp))
            Button(onClick = onAdd, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Add")
            }
        }
    }
}
