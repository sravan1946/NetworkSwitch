package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Rises and fades the content in on first composition, [index] steps after the first
 * item, so a screen assembles top to bottom instead of popping in at once.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    val spatial = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val effects = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        launch { alpha.animateTo(1f, effects) }
        progress.animateTo(1f, spatial)
    }
    val rise = with(LocalDensity.current) { 48.dp.toPx() }
    return graphicsLayer {
        this.alpha = alpha.value
        translationY = (1f - progress.value) * rise
        val scale = 0.94f + 0.06f * progress.value
        scaleX = scale
        scaleY = scale
    }
}

/** Horizontal "no" shake; the caller bumps a key to trigger it. */
suspend fun Animatable<Float, *>.shake() {
    animateTo(0f, keyframes {
        durationMillis = 420
        -18f at 50
        16f at 120
        -12f at 190
        8f at 260
        -4f at 330
    })
}
