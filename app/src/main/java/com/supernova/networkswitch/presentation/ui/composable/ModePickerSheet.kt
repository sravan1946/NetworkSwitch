package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.ModeGroup
import com.supernova.networkswitch.domain.model.NetworkMode
import kotlinx.coroutines.launch

/**
 * Bottom sheet for choosing modes. Single-select picks and closes; multi-select keeps
 * the order modes were ticked in, which becomes the quick-mode order.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModePickerSheet(
    title: String,
    selected: List<NetworkMode>,
    multiSelect: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<NetworkMode>) -> Unit,
    disabled: Set<NetworkMode> = emptySet(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val picks = remember { selected.toMutableStateList() }
    var query by rememberSaveable { mutableStateOf("") }
    var showRegional by rememberSaveable { mutableStateOf(selected.any { it.group == ModeGroup.REGIONAL }) }

    fun close(result: List<NetworkMode>?) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (result != null) onConfirm(result) else onDismiss()
        }
    }

    val matches = NetworkMode.entries.filter {
        query.isBlank() || listOf(it.title, it.technology, it.displayName).any { s -> s.contains(query.trim(), ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search modes") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.size(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            ModeGroup.entries.forEach { group ->
                val modes = matches.filter { it.group == group }
                    .sortedWith(compareByDescending<NetworkMode> { it.highestGeneration }.thenBy { it.generations.size })
                if (modes.isEmpty()) return@forEach
                val collapsible = group == ModeGroup.REGIONAL && query.isBlank()
                item(key = "header-$group") {
                    GroupHeader(
                        group.label,
                        expanded = if (collapsible) showRegional else null,
                        onToggle = { showRegional = !showRegional },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (!collapsible || showRegional) {
                    items(modes, key = { it.name }) { mode ->
                        ModeRow(
                            mode = mode,
                            checked = mode in picks,
                            multiSelect = multiSelect,
                            enabled = mode !in disabled,
                            onClick = {
                                if (multiSelect) {
                                    if (mode in picks) picks.remove(mode) else picks.add(mode)
                                } else {
                                    close(listOf(mode))
                                }
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        if (multiSelect) {
            Button(
                onClick = { close(picks.toList()) },
                enabled = picks.isNotEmpty(),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .heightIn(ButtonDefaults.MediumContainerHeight)
                    .navigationBarsPadding(),
            ) {
                Text(if (picks.isEmpty()) "Pick at least one" else "Pin ${picks.size} mode${if (picks.size == 1) "" else "s"}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupHeader(label: String, expanded: Boolean?, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val turn by animateFloatAsState(if (expanded == true) 180f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "chevron")
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (expanded != null) {
                Icon(Icons.Rounded.ExpandMore, contentDescription = if (expanded) "Collapse" else "Expand", modifier = Modifier.graphicsLayer { rotationZ = turn })
            }
        }
    }
    if (expanded == null) {
        Surface(color = Color.Transparent, modifier = modifier) { content() }
    } else {
        Surface(onClick = onToggle, shape = MaterialTheme.shapes.large, color = Color.Transparent, modifier = modifier) { content() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModeRow(
    mode: NetworkMode,
    checked: Boolean,
    multiSelect: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (checked) colors.secondaryContainer else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "rowContainer",
    )
    // Selected rows get the rounder shape, echoing expressive list selection.
    val corner by animateFloatAsState(if (checked) 28f else 16f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "rowCorner")

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(corner.dp),
        color = container,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f },
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ModeBadge(
                mode,
                container = if (checked) colors.primary else colors.secondaryContainer,
                content = if (checked) colors.onPrimary else colors.onSecondaryContainer,
            )
            Column(Modifier.weight(1f)) {
                Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(mode.technology, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (multiSelect) {
                Checkbox(checked = checked, onCheckedChange = null)
            } else {
                RadioButton(selected = checked, onClick = null)
            }
        }
    }
}
