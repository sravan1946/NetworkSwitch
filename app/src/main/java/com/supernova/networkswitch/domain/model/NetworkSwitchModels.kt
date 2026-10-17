package com.supernova.networkswitch.domain.model

enum class ControlMethod {
    ROOT,
    SHIZUKU
}

enum class NetworkMode(val displayName: String, val value: Int) {
    // Basic modes
    GSM_ONLY("2G Only (GSM)", 1),
    WCDMA_ONLY("3G Only (WCDMA)", 2),
    LTE_ONLY("4G Only (LTE)", 11),
    NR_ONLY("5G Only (NR)", 23),
    
    // Preferred modes  
    WCDMA_PREF("2G/3G (3G Preferred)", 0),
    GSM_UMTS("2G/3G (Auto)", 3),
    
    // Combined modes
    LTE_GSM_WCDMA("2G/3G/4G (LTE/GSM/WCDMA)", 9),
    LTE_WCDMA("3G/4G (LTE/WCDMA)", 12),
    NR_LTE("4G/5G (NR/LTE)", 24),
    NR_LTE_GSM_WCDMA("2G/3G/4G/5G (NR/LTE/GSM/WCDMA)", 26),
    NR_LTE_WCDMA("3G/4G/5G (NR/LTE/WCDMA)", 28),
    
    // CDMA modes for US carriers
    CDMA("CDMA (Auto)", 4),
    CDMA_NO_EVDO("CDMA Only", 5),
    EVDO_NO_CDMA("EvDo Only", 6),
    LTE_CDMA_EVDO("CDMA/4G (LTE/CDMA/EvDo)", 8),
    NR_LTE_CDMA_EVDO("CDMA/4G/5G (NR/LTE/CDMA/EvDo)", 25),
    
    // Global modes
    GLOBAL("Global (All)", 7),
    LTE_CDMA_EVDO_GSM_WCDMA("Global 4G (LTE/CDMA/EvDo/GSM/WCDMA)", 10),
    NR_LTE_CDMA_EVDO_GSM_WCDMA("Global 5G (NR/LTE/CDMA/EvDo/GSM/WCDMA)", 27),
    
    // TD-SCDMA modes for China
    TDSCDMA_ONLY("TD-SCDMA Only", 13),
    TDSCDMA_WCDMA("3G (TD-SCDMA/WCDMA)", 14),
    LTE_TDSCDMA("4G (LTE/TD-SCDMA)", 15),
    TDSCDMA_GSM("2G/TD-SCDMA", 16),
    LTE_TDSCDMA_GSM("2G/4G (LTE/TD-SCDMA/GSM)", 17),
    TDSCDMA_GSM_WCDMA("2G/3G (TD-SCDMA/GSM/WCDMA)", 18),
    LTE_TDSCDMA_WCDMA("3G/4G (LTE/TD-SCDMA/WCDMA)", 19),
    LTE_TDSCDMA_GSM_WCDMA("2G/3G/4G (LTE/TD-SCDMA/GSM/WCDMA)", 20),
    TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 3G (TD-SCDMA/CDMA/EvDo/GSM/WCDMA)", 21),
    LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 4G (LTE/TD-SCDMA/CDMA/EvDo/GSM/WCDMA)", 22),
    NR_LTE_TDSCDMA("4G/5G (NR/LTE/TD-SCDMA)", 29),
    NR_LTE_TDSCDMA_GSM("2G/4G/5G (NR/LTE/TD-SCDMA/GSM)", 30),
    NR_LTE_TDSCDMA_WCDMA("3G/4G/5G (NR/LTE/TD-SCDMA/WCDMA)", 31),
    NR_LTE_TDSCDMA_GSM_WCDMA("2G/3G/4G/5G (NR/LTE/TD-SCDMA/GSM/WCDMA)", 32),
    NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 5G + TD-SCDMA (All Networks)", 33);
    
    /** Radio generations (2–5) this mode allows, derived from the RAT tokens in its name. */
    val generations: Set<Int>
        get() = when (this) {
            // Names that do not spell out their RATs.
            WCDMA_PREF, GSM_UMTS, CDMA, GLOBAL -> setOf(2, 3)
            CDMA_NO_EVDO -> setOf(2)
            EVDO_NO_CDMA -> setOf(3)
            else -> name.split('_').mapNotNull {
                when (it) {
                    "NR" -> 5
                    "LTE" -> 4
                    "WCDMA", "TDSCDMA", "EVDO" -> 3
                    "GSM", "CDMA" -> 2
                    else -> null
                }
            }.toSet()
        }

    val highestGeneration: Int get() = generations.max()

    /** Compact label for tiles and chips: "5G", "4G + 5G", "2G–5G". */
    val shortLabel: String
        get() {
            val gens = generations.sorted()
            return when {
                gens.size == 1 -> "${gens[0]}G"
                gens.size == 2 -> "${gens[0]}G + ${gens[1]}G"
                else -> "${gens.first()}G–${gens.last()}G"
            }
        }

    /** Human title for lists: "5G only", "4G + 5G". */
    val title: String get() = if (generations.size == 1) "$shortLabel only" else shortLabel

    /** Radio technologies, e.g. "NR/LTE", taken from the display name's parenthetical. */
    val technology: String
        get() = Regex("""\((.*)\)""").find(displayName)?.groupValues?.get(1) ?: displayName

    val group: ModeGroup
        get() = when {
            this in COMMON_MODES -> ModeGroup.COMMON
            name.contains("CDMA") || name.contains("EVDO") || this == GLOBAL -> ModeGroup.REGIONAL
            else -> ModeGroup.OLDER
        }

    companion object {
        /**
         * Get NetworkMode by RIL constant value
         */
        fun fromValue(value: Int): NetworkMode? {
            return values().find { it.value == value }
        }

        private val COMMON_MODES = setOf(NR_ONLY, NR_LTE, LTE_ONLY, NR_LTE_GSM_WCDMA, LTE_GSM_WCDMA)
    }
}

/** How the mode picker buckets modes; regional (CDMA, TD-SCDMA) modes are tucked away. */
enum class ModeGroup(val label: String) {
    COMMON("Common"),
    OLDER("Older networks"),
    REGIONAL("Other regions (CDMA, TD-SCDMA)")
}

/**
 * Configuration for the two modes that can be toggled between
 */
data class ToggleModeConfig(
    val modeA: NetworkMode,
    val modeB: NetworkMode,
    val nextModeIsB: Boolean = true
) {
    fun getNextMode(): NetworkMode {
        return if (nextModeIsB) modeB else modeA
    }
    
    fun getCurrentMode(): NetworkMode {
        return if (nextModeIsB) modeA else modeB
    }
    
    fun toggle(): ToggleModeConfig {
        return copy(nextModeIsB = !nextModeIsB)
    }

    /** The other side of the pair from [current]; falls back to the stored flag when unknown. */
    fun nextModeFrom(current: NetworkMode?): NetworkMode = when (current) {
        modeA -> modeB
        modeB -> modeA
        else -> getNextMode()
    }

    /** Records that [mode] is now active so the flag stays right for the fallback path. */
    fun after(mode: NetworkMode): ToggleModeConfig = when (mode) {
        modeA -> copy(nextModeIsB = true)
        modeB -> copy(nextModeIsB = false)
        else -> this
    }
}

sealed class CompatibilityState {
    object Pending : CompatibilityState()
    object Compatible : CompatibilityState()
    data class Incompatible(val reason: String) : CompatibilityState()
    data class PermissionDenied(val method: ControlMethod) : CompatibilityState()
}
