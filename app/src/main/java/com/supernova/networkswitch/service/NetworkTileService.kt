package com.supernova.networkswitch.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import android.util.Log
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.presentation.ui.activity.MainActivity
import com.supernova.networkswitch.presentation.ui.iconRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class NetworkTileService : TileService() {

    @Inject
    lateinit var getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase

    @Inject
    lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase

    @Inject
    lateinit var checkCompatibilityUseCase: CheckCompatibilityUseCase

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    /**
     * Scoped to the listening window: `qsTile` is only non-null between
     * onStartListening and onStopListening, so work outliving the service cannot
     * update the tile.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observeJob: Job? = null

    private var currentNetworkMode: NetworkMode? = null
    private var toggleConfig: ToggleModeConfig? = null
    private var ready = true
    private var switching = false

    override fun onTileAdded() {
        super.onTileAdded()
        serviceScope.launch { preferencesRepository.setTileAdded(true) }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        serviceScope.launch { preferencesRepository.setTileAdded(false) }
    }

    override fun onStartListening() {
        super.onStartListening()
        observeJob?.cancel()
        observeJob = serviceScope.launch {
            // A tile that is listening is evidently added, even from before this flag existed.
            if (!preferencesRepository.observeTileAdded().first()) preferencesRepository.setTileAdded(true)
            try {
                preferencesRepository.observeToggleModeConfig().collect { newConfig ->
                    toggleConfig = newConfig
                    refreshNetworkState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tile: Failed to observe toggle config", e)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        observeJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        if (switching) return

        if (!ready) {
            openApp()
            return
        }

        // Unavailable while the radio works: blocks double taps and dims the tile as feedback.
        switching = true
        updateTileState()

        if (isLocked) {
            unlockAndRun { performToggle() }
        } else {
            performToggle()
        }
    }

    private fun performToggle() {
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()

        serviceScope.launch {
            try {
                // The binder call into the root/Shizuku process can hang indefinitely.
                withTimeout(TOGGLE_TIMEOUT_MS) {
                    toggleNetworkModeUseCase(subId)
                        .onSuccess { newMode -> currentNetworkMode = newMode }
                        .onFailure { e -> Log.e(TAG, "Tile: Toggle failed", e) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tile: Error toggling network mode", e)
            } finally {
                switching = false
                refreshNetworkState()
            }
        }
    }

    private suspend fun refreshNetworkState() {
        ready = runCatching { checkCompatibilityUseCase() is CompatibilityState.Compatible }.getOrDefault(false)
        if (ready) {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            try {
                getCurrentNetworkModeUseCase(subId)
                    .onSuccess { networkMode -> currentNetworkMode = networkMode }
                    .onFailure { e -> Log.e(TAG, "Tile: Failed to read network mode", e) }
            } catch (e: Exception) {
                Log.e(TAG, "Tile: Error refreshing network state", e)
            }
        }

        withContext(Dispatchers.Main) { updateTileState() }
    }

    private fun updateTileState() {
        try {
            // Null outside the listening window; the next onStartListening repaints.
            val tile = qsTile ?: return
            val config = toggleConfig
            val mode = currentNetworkMode

            when {
                config == null -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = getString(R.string.network_switch)
                    tile.subtitle = getString(R.string.tile_not_loaded)
                }
                !ready -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = getString(R.string.network_switch)
                    tile.subtitle = getString(R.string.tile_needs_setup)
                }
                switching -> {
                    tile.state = Tile.STATE_UNAVAILABLE
                    tile.subtitle = getString(R.string.tile_switching)
                }
                else -> {
                    // Lit only on the pair's second mode, so the tile reads as an on/off switch.
                    tile.state = if (mode == config.modeB) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.label = mode?.shortLabel ?: getString(R.string.network_switch)
                    tile.subtitle = getString(R.string.tile_tap_for, config.nextModeFrom(mode).shortLabel)
                }
            }
            tile.icon = Icon.createWithResource(this, mode.iconRes())
            tile.updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "Tile: Failed to update tile state", e)
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "NetworkSwitch"
        const val TOGGLE_TIMEOUT_MS = 10_000L
    }
}
