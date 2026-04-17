package app.portfoliotracker.data.repository

import app.portfoliotracker.data.database.PortfolioDatabase

class SettingsRepository(private val db: PortfolioDatabase) {

    fun get(key: String): String? {
        return db.portfolioDatabaseQueries.selectSetting(key)
            .executeAsOneOrNull()
    }

    fun set(key: String, value: String) {
        db.portfolioDatabaseQueries.upsertSetting(key, value)
    }

    fun getBaseCurrency(): String = get(KEY_BASE_CURRENCY) ?: DEFAULT_BASE_CURRENCY

    fun setBaseCurrency(currency: String) = set(KEY_BASE_CURRENCY, currency)

    fun getRefreshIntervalHours(): Int {
        return get(KEY_REFRESH_INTERVAL)?.toIntOrNull() ?: DEFAULT_REFRESH_HOURS
    }

    fun setRefreshIntervalHours(hours: Int) = set(KEY_REFRESH_INTERVAL, hours.toString())

    fun getFinnhubApiKey(): String = get(KEY_FINNHUB_API_KEY) ?: ""

    fun setFinnhubApiKey(key: String) = set(KEY_FINNHUB_API_KEY, key)

    companion object {
        const val KEY_BASE_CURRENCY = "base_currency"
        const val KEY_REFRESH_INTERVAL = "refresh_interval_hours"
        const val KEY_FINNHUB_API_KEY = "finnhub_api_key"
        const val DEFAULT_BASE_CURRENCY = "EUR"
        const val DEFAULT_REFRESH_HOURS = 24
    }
}
