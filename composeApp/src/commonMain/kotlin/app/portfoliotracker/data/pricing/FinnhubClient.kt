package app.portfoliotracker.data.pricing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

class FinnhubClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    suspend fun getQuote(ticker: String): Double? {
        return try {
            val response: FinnhubQuoteResponse = httpClient.get("https://finnhub.io/api/v1/quote") {
                header("X-Finnhub-Token", apiKey)
                parameter("symbol", ticker)
            }.body()
            if (response.c > 0) response.c else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getQuotes(tickers: List<String>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (ticker in tickers) {
            val price = getQuote(ticker)
            if (price != null) result[ticker] = price
            delay(100) // rate limit: 60 req/min
        }
        return result
    }
}

@Serializable
data class FinnhubQuoteResponse(
    val c: Double = 0.0,  // current price
    val d: Double = 0.0,  // change
    val dp: Double = 0.0, // percent change
    val h: Double = 0.0,  // high
    val l: Double = 0.0,  // low
    val o: Double = 0.0,  // open
    val pc: Double = 0.0, // previous close
)
