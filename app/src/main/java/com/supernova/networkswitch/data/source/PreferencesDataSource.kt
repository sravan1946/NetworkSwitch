package com.supernova.networkswitch.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val CONTROL_METHOD_KEY = stringPreferencesKey("control_method")
        private val TOGGLE_MODE_A_KEY = intPreferencesKey("toggle_mode_a")
        private val TOGGLE_MODE_B_KEY = intPreferencesKey("toggle_mode_b")
        private val TOGGLE_NEXT_IS_B_KEY = booleanPreferencesKey("toggle_next_is_b")
        private val PINNED_MODES_KEY = stringPreferencesKey("pinned_modes")
        private val TILE_ADDED_KEY = booleanPreferencesKey("tile_added")
        
        private const val DEFAULT_CONTROL_METHOD = "SHIZUKU"
        
        private val DEFAULT_MODE_A = NetworkMode.LTE_ONLY
        private val DEFAULT_MODE_B = NetworkMode.NR_ONLY
        private const val DEFAULT_NEXT_IS_B = true
        private val DEFAULT_PINNED_MODES = listOf(NetworkMode.LTE_ONLY, NetworkMode.NR_LTE, NetworkMode.NR_ONLY)
    }
    
    private fun parseControlMethod(methodString: String?): ControlMethod {
        return try {
            ControlMethod.valueOf(methodString ?: DEFAULT_CONTROL_METHOD)
        } catch (e: IllegalArgumentException) {
            ControlMethod.SHIZUKU
        }
    }
    
    suspend fun getControlMethod(): ControlMethod {
        return dataStore.data.map { preferences ->
            parseControlMethod(preferences[CONTROL_METHOD_KEY])
        }.first()
    }
    
    suspend fun setControlMethod(method: ControlMethod) {
        dataStore.edit { preferences ->
            preferences[CONTROL_METHOD_KEY] = method.name
        }
    }
    
    fun observeControlMethod(): Flow<ControlMethod> {
        return dataStore.data.map { preferences ->
            parseControlMethod(preferences[CONTROL_METHOD_KEY])
        }
    }
    
    suspend fun getToggleModeConfig(): ToggleModeConfig {
        return dataStore.data.map { preferences ->
            val modeAValue = preferences[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value
            val modeBValue = preferences[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value
            val nextIsB = preferences[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B
            
            val modeA = NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A
            val modeB = NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B
            
            ToggleModeConfig(modeA, modeB, nextIsB)
        }.first()
    }
    
    suspend fun setToggleModeConfig(config: ToggleModeConfig) {
        dataStore.edit { preferences ->
            preferences[TOGGLE_MODE_A_KEY] = config.modeA.value
            preferences[TOGGLE_MODE_B_KEY] = config.modeB.value
            preferences[TOGGLE_NEXT_IS_B_KEY] = config.nextModeIsB
        }
    }
    
    fun observeToggleModeConfig(): Flow<ToggleModeConfig> {
        return dataStore.data.map { preferences ->
            val modeAValue = preferences[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value
            val modeBValue = preferences[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value
            val nextIsB = preferences[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B
            
            val modeA = NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A
            val modeB = NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B
            
            ToggleModeConfig(modeA, modeB, nextIsB)
        }
    }

    /** Stored as comma-separated RIL values; unknown values are dropped. */
    fun observePinnedModes(): Flow<List<NetworkMode>> {
        return dataStore.data.map { preferences ->
            preferences[PINNED_MODES_KEY]
                ?.split(',')
                ?.mapNotNull { it.toIntOrNull()?.let(NetworkMode::fromValue) }
                ?: DEFAULT_PINNED_MODES
        }
    }

    suspend fun setPinnedModes(modes: List<NetworkMode>) {
        dataStore.edit { preferences ->
            preferences[PINNED_MODES_KEY] = modes.joinToString(",") { it.value.toString() }
        }
    }

    fun observeTileAdded(): Flow<Boolean> {
        return dataStore.data.map { preferences -> preferences[TILE_ADDED_KEY] ?: false }
    }

    suspend fun setTileAdded(added: Boolean) {
        dataStore.edit { preferences -> preferences[TILE_ADDED_KEY] = added }
    }
}
