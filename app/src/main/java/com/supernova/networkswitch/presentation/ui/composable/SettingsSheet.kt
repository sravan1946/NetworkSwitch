package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.supernova.networkswitch.BuildConfig
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(onDismiss: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val method by viewModel.controlMethod.collectAsState()
    val uri = LocalUriHandler.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            SectionHeader("Control method", Modifier.padding(top = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MethodOption(
                    title = "Shizuku",
                    detail = "No root needed. Shizuku must be running.",
                    state = viewModel.shizukuCompatibility,
                    selected = method == ControlMethod.SHIZUKU,
                    onClick = { viewModel.updateControlMethod(ControlMethod.SHIZUKU) },
                )
                MethodOption(
                    title = "Root",
                    detail = "For rooted phones (Magisk, KernelSU).",
                    state = viewModel.rootCompatibility,
                    selected = method == ControlMethod.ROOT,
                    onClick = { viewModel.updateControlMethod(ControlMethod.ROOT) },
                )
            }

            SectionHeader("About", Modifier.padding(top = 16.dp))
            LinkRow("Source code", "github.com/sravan1946/NetworkSwitch") { uri.openUri("https://github.com/sravan1946/NetworkSwitch") }
            LinkRow("Original project", "github.com/aunchagaonkar/NetworkSwitch") { uri.openUri("https://github.com/aunchagaonkar/NetworkSwitch") }
            Text(
                "Version ${BuildConfig.VERSION_NAME} · GPL-3.0\n" +
                    "Uses Shizuku, libsu, Android Jetpack, Kotlin and Material Symbols (Apache 2.0).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MethodOption(title: String, detail: String, state: CompatibilityState, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(if (selected) colors.secondaryContainer else colors.surfaceContainerHigh, effects, label = "methodContainer")
    val border by animateColorAsState(if (selected) colors.secondary else Color.Transparent, effects, label = "methodBorder")

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        border = BorderStroke(2.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            StatusPill(state)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusPill(state: CompatibilityState) {
    val colors = MaterialTheme.colorScheme
    if (state is CompatibilityState.Pending) {
        LoadingIndicator(Modifier.size(28.dp))
        return
    }
    val (label, bg, fg) = when (state) {
        is CompatibilityState.Compatible -> Triple("Ready", colors.primary, colors.onPrimary)
        is CompatibilityState.PermissionDenied -> Triple("No access", colors.errorContainer, colors.onErrorContainer)
        else -> Triple("Unavailable", colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
    Surface(shape = CircleShape, color = bg, contentColor = fg) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
