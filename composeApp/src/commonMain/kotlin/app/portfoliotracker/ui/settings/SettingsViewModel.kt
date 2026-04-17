package app.portfoliotracker.ui.settings

import app.portfoliotracker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
) {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    suspend fun load() {
        _state.value = SettingsState(
            baseCurrency = settingsRepo.getBaseCurrency(),
            refreshIntervalHours = settingsRepo.getRefreshIntervalHours(),
            finnhubApiKey = settingsRepo.getFinnhubApiKey(),
        )
    }

    suspend fun setBaseCurrency(currency: String) {
        settingsRepo.setBaseCurrency(currency)
        _state.value = _state.value.copy(baseCurrency = currency)
    }

    suspend fun setRefreshIntervalHours(hours: Int) {
        settingsRepo.setRefreshIntervalHours(hours)
        _state.value = _state.value.copy(refreshIntervalHours = hours)
    }

    suspend fun setFinnhubApiKey(key: String) {
        settingsRepo.setFinnhubApiKey(key)
        _state.value = _state.value.copy(finnhubApiKey = key)
    }
}

data class SettingsState(
    val baseCurrency: String = "EUR",
    val refreshIntervalHours: Int = 24,
    val finnhubApiKey: String = "",
)
