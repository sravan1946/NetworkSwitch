package com.supernova.networkswitch.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.SetNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import javax.inject.Inject

/** Where Shizuku setup stands; each flag implies the ones before it. */
data class ShizukuSetup(val installed: Boolean, val running: Boolean, val granted: Boolean)

/** One-off outcomes the screen reacts to with haptics, motion and a snackbar. */
sealed interface SwitchEvent {
    data class Switched(val mode: NetworkMode) : SwitchEvent
    data class Failed(val message: String) : SwitchEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val checkCompatibilityUseCase: CheckCompatibilityUseCase,
    private val getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase,
    private val toggleNetworkModeUseCase: ToggleNetworkModeUseCase,
    private val setNetworkModeUseCase: SetNetworkModeUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    var selectedMethod by mutableStateOf(ControlMethod.SHIZUKU)
        private set

    var compatibilityState by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set

    var shizukuSetup by mutableStateOf(ShizukuSetup(installed = false, running = false, granted = false))
        private set

    var currentNetworkMode by mutableStateOf<NetworkMode?>(null)
        private set

    var toggleModeConfig by mutableStateOf(ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY))
        private set

    var pinnedModes by mutableStateOf<List<NetworkMode>>(emptyList())
        private set

    var tileAdded by mutableStateOf(true)
        private set

    /** The mode being applied, or null when idle. */
    var switchingTo by mutableStateOf<NetworkMode?>(null)
        private set

    val nextMode: NetworkMode get() = toggleModeConfig.nextModeFrom(currentNetworkMode)

    private val _events = MutableSharedFlow<SwitchEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SwitchEvent> = _events

    // Shizuku calls back on the main thread once the user answers its permission dialog.
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ -> refreshAllData() }

    // The binder reaches this process a moment after Shizuku starts, often after onResume
    // already checked; re-check when it arrives or dies.
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshAllData() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshAllData() }

    init {
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        viewModelScope.launch {
            preferencesRepository.observeControlMethod().collectLatest { method ->
                selectedMethod = method
                refreshAllData()
            }
        }
        viewModelScope.launch {
            preferencesRepository.observeToggleModeConfig().collectLatest { toggleModeConfig = it }
        }
        viewModelScope.launch {
            preferencesRepository.observePinnedModes().collectLatest { pinnedModes = it }
        }
        viewModelScope.launch {
            preferencesRepository.observeTileAdded().collectLatest { tileAdded = it }
        }
    }

    override fun onCleared() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }

    fun refreshAllData() {
        viewModelScope.launch {
            shizukuSetup = readShizukuSetup()
            compatibilityState = checkCompatibilityUseCase()
            if (compatibilityState is CompatibilityState.Compatible) refreshNetworkState()
        }
    }

    fun toggleNetworkMode() = switch(nextMode) { subId -> toggleNetworkModeUseCase(subId) }

    fun applyMode(mode: NetworkMode) {
        if (mode == currentNetworkMode) return
        switch(mode) { subId -> setNetworkModeUseCase(subId, mode) }
    }

    private fun switch(target: NetworkMode, action: suspend (Int) -> Result<NetworkMode>) {
        if (switchingTo != null) return
        switchingTo = target
        viewModelScope.launch {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            action(subId)
                .onSuccess { mode ->
                    currentNetworkMode = mode
                    _events.tryEmit(SwitchEvent.Switched(mode))
                }
                .onFailure { e ->
                    _events.tryEmit(SwitchEvent.Failed(e.message ?: "Couldn't switch network mode"))
                }
            // Read back what the radio actually accepted.
            refreshNetworkState()
            switchingTo = null
        }
    }

    fun setPair(modeA: NetworkMode, modeB: NetworkMode) {
        viewModelScope.launch {
            preferencesRepository.setToggleModeConfig(ToggleModeConfig(modeA, modeB).after(currentNetworkMode ?: modeA))
        }
    }

    fun updatePinnedModes(modes: List<NetworkMode>) {
        viewModelScope.launch { preferencesRepository.setPinnedModes(modes) }
    }

    fun markTileAdded() {
        viewModelScope.launch { preferencesRepository.setTileAdded(true) }
    }

    fun requestShizukuPermission() {
        runCatching { Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST) }
            .onFailure { refreshAllData() }
    }

    fun shizukuLaunchIntent(): Intent? = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)

    private fun readShizukuSetup(): ShizukuSetup {
        val installed = try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = running && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        // A running binder proves Shizuku exists even if package visibility hides it.
        return ShizukuSetup(installed || running, running, granted)
    }

    private suspend fun refreshNetworkState() {
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        currentNetworkMode = getCurrentNetworkModeUseCase(subId).getOrNull()
    }

    private companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val SHIZUKU_PERMISSION_REQUEST = 8
    }
}
