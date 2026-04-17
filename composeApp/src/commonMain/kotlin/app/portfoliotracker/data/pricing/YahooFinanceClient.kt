package app.portfoliotracker.data.pricing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class YahooFinanceClient(private val httpClient: HttpClient) {

    suspend fun getQuote(ticker: String): Double? {
        return try {
            val response: JsonObject = httpClient.get(
                "https://query1.finance.yahoo.com/v8/finance/chart/$ticker"
            ).body()
            val meta = response["chart"]
                ?.jsonObject?.get("result")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("meta")
                ?.jsonObject
            meta?.get("regularMarketPrice")?.jsonPrimitive?.double
        } catch (_: Exception) {
            null
        }
    }
}
