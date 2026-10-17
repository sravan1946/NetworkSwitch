package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.presentation.viewmodel.ShizukuSetup

private data class Step(
    val title: String,
    val detail: String,
    val done: Boolean,
    val actionLabel: String,
    val onAction: () -> Unit,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SetupCard(
    method: ControlMethod,
    state: CompatibilityState,
    shizuku: ShizukuSetup,
    onGetShizuku: () -> Unit,
    onOpenShizuku: () -> Unit,
    onGrantShizuku: () -> Unit,
    onRetry: () -> Unit,
    onSwitchMethod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = when (method) {
        ControlMethod.SHIZUKU -> listOf(
            Step("Install Shizuku", "It lets apps use system APIs without root.", shizuku.installed, "Get it", onGetShizuku),
            Step("Start Shizuku", "Open Shizuku and start it with wireless debugging or root.", shizuku.running, "Open", onOpenShizuku),
            Step("Allow Network Switch", "Grant access when Shizuku asks.", shizuku.granted, "Allow", onGrantShizuku),
        )
        ControlMethod.ROOT -> listOf(
            Step("Grant root access", "Approve Network Switch in your root manager.", false, "Try again", onRetry),
        )
    }
    val current = steps.indexOfFirst { !it.done }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val spin = rememberInfiniteTransition(label = "setupSpin")
            .animateFloat(0f, 360f, infiniteRepeatable(tween(12_000)), label = "setupSpinAngle")
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(120.dp)
                    .graphicsLayer { rotationZ = spin.value }
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.size(20.dp))
        Text("Almost there", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (method == ControlMethod.SHIZUKU) "Switching network modes needs Shizuku." else "Switching network modes needs root.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(24.dp))

        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(vertical = 8.dp)) {
                steps.forEachIndexed { i, step ->
                    StepRow(index = i, step = step, isCurrent = i == current)
                }
            }
        }

        // Anything the checklist doesn't explain, such as a failed binder call.
        (state as? CompatibilityState.Incompatible)?.reason
            ?.takeIf { method == ControlMethod.ROOT || shizuku.granted }
            ?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }

        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onSwitchMethod) {
            Text(if (method == ControlMethod.SHIZUKU) "I have root instead" else "Use Shizuku instead")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepRow(index: Int, step: Step, isCurrent: Boolean) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val effects = motion.defaultEffectsSpec<Color>()
    val badge by animateColorAsState(
        when {
            step.done -> colors.primary
            isCurrent -> colors.tertiaryContainer
            else -> colors.surfaceContainerHighest
        },
        effects,
        label = "stepBadge",
    )
    // The step waiting on the user breathes gently to draw the eye.
    val breathe = rememberInfiniteTransition(label = "stepBreathe")
        .animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "stepBreatheScale")

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .graphicsLayer {
                    val s = if (isCurrent) breathe.value else 1f
                    scaleX = s
                    scaleY = s
                }
                .clip((if (step.done) MaterialShapes.Cookie9Sided else MaterialShapes.Circle).toShape())
                .background(badge),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = step.done,
                transitionSpec = {
                    (scaleIn(motion.fastSpatialSpec()) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "stepIcon",
            ) { done ->
                if (done) {
                    Icon(Icons.Rounded.Check, contentDescription = "Done", tint = colors.onPrimary)
                } else {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) colors.onTertiaryContainer else colors.onSurfaceVariant,
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(step.detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (isCurrent) {
            Button(onClick = step.onAction, shapes = ButtonDefaults.shapes()) { Text(step.actionLabel) }
        }
    }
}
