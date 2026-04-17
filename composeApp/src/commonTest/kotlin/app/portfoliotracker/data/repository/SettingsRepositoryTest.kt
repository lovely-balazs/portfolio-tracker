package app.portfoliotracker.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {

    @Test
    fun defaultBaseCurrencyIsEUR() {
        assertEquals("EUR", SettingsRepository.DEFAULT_BASE_CURRENCY)
    }

    @Test
    fun defaultRefreshIntervalIs24Hours() {
        assertEquals(24, SettingsRepository.DEFAULT_REFRESH_HOURS)
    }

    @Test
    fun settingKeysAreDistinct() {
        val keys = listOf(
            SettingsRepository.KEY_BASE_CURRENCY,
            SettingsRepository.KEY_REFRESH_INTERVAL,
            SettingsRepository.KEY_FINNHUB_API_KEY,
        )
        assertEquals(keys.size, keys.toSet().size)
    }
}
