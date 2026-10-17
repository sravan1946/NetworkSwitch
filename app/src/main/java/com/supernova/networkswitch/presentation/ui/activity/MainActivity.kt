package com.supernova.networkswitch.presentation.ui.activity

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.ui.composable.AddTileBanner
import com.supernova.networkswitch.presentation.ui.composable.HeroModeCard
import com.supernova.networkswitch.presentation.ui.composable.ModePickerSheet
import com.supernova.networkswitch.presentation.ui.composable.PinnedModesSection
import com.supernova.networkswitch.presentation.ui.composable.SettingsSheet
import com.supernova.networkswitch.presentation.ui.composable.SetupCard
import com.supernova.networkswitch.presentation.ui.composable.TilePairSection
import com.supernova.networkswitch.presentation.ui.composable.staggeredEntrance
import com.supernova.networkswitch.presentation.viewmodel.MainViewModel
import com.supernova.networkswitch.presentation.viewmodel.SwitchEvent
import com.supernova.networkswitch.service.NetworkTileService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NetworkSwitchTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAllData()
    }
}

private enum class Picker { SLOT_A, SLOT_B, PINS }

/** What the body shows; a separate key so AnimatedContent only animates real changes. */
private enum class Stage { CHECKING, SETUP, READY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbar = remember { SnackbarHostState() }
    var picker by rememberSaveable { mutableStateOf<Picker?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val state = viewModel.compatibilityState
    val stage = when (state) {
        CompatibilityState.Pending -> if (viewModel.currentNetworkMode == null) Stage.CHECKING else Stage.READY
        CompatibilityState.Compatible -> Stage.READY
        else -> Stage.SETUP
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { if (it is SwitchEvent.Failed) snackbar.showSnackbar(it.message) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Network Switch") },
                subtitle = { Text(if (stage == Stage.READY) "Tap the shape to switch" else "Let's get set up") },
                actions = {
                    StatusChip(
                        method = viewModel.selectedMethod,
                        ready = state is CompatibilityState.Compatible,
                        onClick = { showSettings = true },
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val motion = MaterialTheme.motionScheme
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (fadeIn(motion.slowEffectsSpec()) + scaleIn(motion.slowSpatialSpec(), 0.92f)) togetherWith
                    (fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), 1.04f))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "stage",
        ) { current ->
            when (current) {
                Stage.CHECKING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingIndicator(Modifier.size(72.dp))
                        Text("Checking access…", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                    }
                }
                Stage.SETUP -> Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    SetupCard(
                        method = viewModel.selectedMethod,
                        state = state,
                        shizuku = viewModel.shizukuSetup,
                        onGetShizuku = { uri.openUri("https://shizuku.rikka.app/download/") },
                        onOpenShizuku = {
                            viewModel.shizukuLaunchIntent()?.let(context::startActivity)
                                ?: uri.openUri("https://shizuku.rikka.app/guide/setup/")
                        },
                        onGrantShizuku = viewModel::requestShizukuPermission,
                        onRetry = viewModel::refreshAllData,
                        onSwitchMethod = { showSettings = true },
                        modifier = Modifier.staggeredEntrance(0),
                    )
                }
                Stage.READY -> ReadyContent(viewModel, onPick = { picker = it })
            }
        }
    }

    val config = viewModel.toggleModeConfig
    when (picker) {
        Picker.SLOT_A -> ModePickerSheet(
            title = "When the tile is off",
            selected = listOf(config.modeA),
            multiSelect = false,
            disabled = setOf(config.modeB),
            onDismiss = { picker = null },
            onConfirm = { viewModel.setPair(it.first(), config.modeB); picker = null },
        )
        Picker.SLOT_B -> ModePickerSheet(
            title = "When the tile is on",
            selected = listOf(config.modeB),
            multiSelect = false,
            disabled = setOf(config.modeA),
            onDismiss = { picker = null },
            onConfirm = { viewModel.setPair(config.modeA, it.first()); picker = null },
        )
        Picker.PINS -> ModePickerSheet(
            title = "Quick modes",
            selected = viewModel.pinnedModes,
            multiSelect = true,
            onDismiss = { picker = null },
            onConfirm = { viewModel.updatePinnedModes(it); picker = null },
        )
        null -> Unit
    }
    if (showSettings) SettingsSheet(onDismiss = { showSettings = false })
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReadyContent(viewModel: MainViewModel, onPick: (Picker) -> Unit) {
    val context = LocalContext.current
    val config = viewModel.toggleModeConfig
    val current = viewModel.currentNetworkMode

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        HeroModeCard(
            currentMode = current,
            nextMode = viewModel.nextMode,
            isOnMode = current == config.modeB,
            switchingTo = viewModel.switchingTo,
            events = viewModel.events,
            onToggle = viewModel::toggleNetworkMode,
            modifier = Modifier
                .padding(top = 8.dp)
                .staggeredEntrance(0),
        )
        PinnedModesSection(
            pinned = viewModel.pinnedModes,
            currentMode = current,
            switchingTo = viewModel.switchingTo,
            onApply = viewModel::applyMode,
            onEdit = { onPick(Picker.PINS) },
            modifier = Modifier.staggeredEntrance(1),
        )
        TilePairSection(
            modeA = config.modeA,
            modeB = config.modeB,
            currentMode = current,
            onPickA = { onPick(Picker.SLOT_A) },
            onPickB = { onPick(Picker.SLOT_B) },
            onSwap = { viewModel.setPair(config.modeB, config.modeA) },
            modifier = Modifier.staggeredEntrance(2),
        )
        AnimatedVisibility(
            visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.tileAdded,
            exit = fadeOut() + shrinkVertically(),
        ) {
            AddTileBanner(
                onAdd = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.getSystemService(StatusBarManager::class.java).requestAddTileService(
                            ComponentName(context, NetworkTileService::class.java),
                            context.getString(R.string.network_switch),
                            Icon.createWithResource(context, R.drawable.ic_mode_5g),
                            context.mainExecutor,
                        ) { result ->
                            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                                result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                            ) viewModel.markTileAdded()
                        }
                    }
                },
                modifier = Modifier.staggeredEntrance(3),
            )
        }
        Spacer(Modifier.size(24.dp))
    }
}

/** Which backend is in use and whether it's live; opens settings. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusChip(method: ControlMethod, ready: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dot by animateColorAsState(if (ready) colors.primary else colors.error, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "dot")
    val pulse = rememberInfiniteTransition(label = "dotPulse")
        .animateFloat(1f, 0.35f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dotAlpha")

    Surface(onClick = onClick, shape = CircleShape, color = colors.surfaceContainerHigh, modifier = modifier) {
        Row(
            Modifier.padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = if (ready) 1f else pulse.value }
                    .clip(CircleShape)
                    .background(dot)
            )
            Text(if (method == ControlMethod.SHIZUKU) "Shizuku" else "Root", style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Rounded.Tune, contentDescription = "Settings", modifier = Modifier.size(18.dp))
        }
    }
}
