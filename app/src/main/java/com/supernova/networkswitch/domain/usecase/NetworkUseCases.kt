package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import javax.inject.Inject

class CheckCompatibilityUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): CompatibilityState {
        val controlMethod = preferencesRepository.getControlMethod()
        return networkControlRepository.checkCompatibility(controlMethod)
    }
}

class ToggleNetworkModeUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(subId: Int): Result<NetworkMode> {
        return try {
            val toggleConfig = preferencesRepository.getToggleModeConfig()

            // Go by the radio's real mode so a change made elsewhere doesn't make the
            // toggle "switch" to the mode already active; the flag covers unreadable modes.
            val current = runCatching { networkControlRepository.getCurrentNetworkMode(subId) }.getOrNull()
            val targetMode = toggleConfig.nextModeFrom(current)

            val result = networkControlRepository.setNetworkMode(subId, targetMode)

            if (result.isSuccess) {
                preferencesRepository.setToggleModeConfig(toggleConfig.after(targetMode))
            }

            result.map { targetMode }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Applies one specific mode (a pinned mode), keeping the toggle pair's flag in step.
 */
class SetNetworkModeUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(subId: Int, mode: NetworkMode): Result<NetworkMode> {
        return try {
            val result = networkControlRepository.setNetworkMode(subId, mode)
            if (result.isSuccess) {
                val config = preferencesRepository.getToggleModeConfig()
                preferencesRepository.setToggleModeConfig(config.after(mode))
            }
            result.map { mode }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for getting current network mode
 */
class GetCurrentNetworkModeUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository
) {
    suspend operator fun invoke(subId: Int): Result<NetworkMode?> {
        return try {
            Result.success(networkControlRepository.getCurrentNetworkMode(subId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for updating control method preference
 */
class UpdateControlMethodUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(method: ControlMethod) {
        preferencesRepository.setControlMethod(method)
    }
}

/**
 * Use case for getting toggle mode configuration
 */
class GetToggleModeConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): ToggleModeConfig {
        return preferencesRepository.getToggleModeConfig()
    }
}

/**
 * Use case for updating toggle mode configuration
 */
class UpdateToggleModeConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(config: ToggleModeConfig) {
        preferencesRepository.setToggleModeConfig(config)
    }
}
