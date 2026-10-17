package com.supernova.networkswitch.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NetworkMode enum
 */
class NetworkModeTest {
    
    @Test
    fun testNetworkModeFromValue() {
        // Test basic modes
        assertEquals(NetworkMode.GSM_ONLY, NetworkMode.fromValue(1))
        assertEquals(NetworkMode.WCDMA_ONLY, NetworkMode.fromValue(2))
        assertEquals(NetworkMode.LTE_ONLY, NetworkMode.fromValue(11))
        assertEquals(NetworkMode.NR_ONLY, NetworkMode.fromValue(23))
        
        // Test combined modes
        assertEquals(NetworkMode.NR_LTE, NetworkMode.fromValue(24))
        assertEquals(NetworkMode.NR_LTE_GSM_WCDMA, NetworkMode.fromValue(26))
        
        // Test invalid value
        assertNull(NetworkMode.fromValue(-1))
        assertNull(NetworkMode.fromValue(999))
    }
    
    @Test
    fun testNetworkModeDisplayNames() {
        assertEquals("2G Only (GSM)", NetworkMode.GSM_ONLY.displayName)
        assertEquals("3G Only (WCDMA)", NetworkMode.WCDMA_ONLY.displayName)
        assertEquals("4G Only (LTE)", NetworkMode.LTE_ONLY.displayName)
        assertEquals("5G Only (NR)", NetworkMode.NR_ONLY.displayName)
        assertEquals("4G/5G (NR/LTE)", NetworkMode.NR_LTE.displayName)
    }

    @Test
    fun testShortLabelsAndGroups() {
        assertEquals("5G", NetworkMode.NR_ONLY.shortLabel)
        assertEquals("4G + 5G", NetworkMode.NR_LTE.shortLabel)
        assertEquals("2G–5G", NetworkMode.NR_LTE_GSM_WCDMA.shortLabel)
        assertEquals("2G + 3G", NetworkMode.GSM_UMTS.shortLabel)
        assertEquals("3G", NetworkMode.EVDO_NO_CDMA.shortLabel)
        assertEquals("5G only", NetworkMode.NR_ONLY.title)
        assertEquals("NR/LTE", NetworkMode.NR_LTE.technology)

        assertEquals(ModeGroup.COMMON, NetworkMode.LTE_ONLY.group)
        assertEquals(ModeGroup.REGIONAL, NetworkMode.TDSCDMA_ONLY.group)
        assertEquals(ModeGroup.REGIONAL, NetworkMode.GLOBAL.group)
        assertEquals(ModeGroup.OLDER, NetworkMode.GSM_ONLY.group)
        // Every mode must resolve to at least one generation.
        NetworkMode.entries.forEach { assertTrue(it.name, it.generations.isNotEmpty()) }
    }
}
