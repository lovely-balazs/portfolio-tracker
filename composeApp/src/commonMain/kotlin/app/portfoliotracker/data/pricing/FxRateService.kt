package app.portfoliotracker.data.pricing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

class FxRateService(private val httpClient: HttpClient) {

    private var cachedRates: FxRates? = null
    private var cacheDate: String? = null

    suspend fun getRates(baseCurrency: String): FxRates {
        val today = currentDate()
        if (cachedRates != null && cacheDate == today && cachedRates?.base == baseCurrency) {
            return cachedRates!!
        }

        return try {
            val response: FrankfurterResponse = httpClient.get(
                "https://api.frankfurter.app/latest"
            ) {
                parameter("from", baseCurrency)
            }.body()

            val rates = FxRates(
                base = baseCurrency,
                rates = response.rates,
            )
            cachedRates = rates
            cacheDate = today
            rates
        } catch (_: Exception) {
            cachedRates ?: FxRates(base = baseCurrency, rates = emptyMap())
        }
    }

    fun convertToBase(amount: Double, fromCurrency: String, rates: FxRates): Double {
        if (fromCurrency == rates.base) return amount
        val rate = rates.rates[fromCurrency] ?: return amount
        return if (rate > 0) amount / rate else amount
    }

    private fun currentDate(): String {
        @OptIn(kotlin.time.ExperimentalTime::class)
        val now = kotlin.time.Clock.System.now()
        return now.toString().take(10) // "2025-01-15T..." → "2025-01-15"
    }
}

data class FxRates(
    val base: String,
    val rates: Map<String, Double>,
)

@Serializable
data class FrankfurterResponse(
    val base: String = "",
    val date: String = "",
    val rates: Map<String, Double> = emptyMap(),
)
