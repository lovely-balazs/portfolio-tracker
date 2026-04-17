package app.portfoliotracker.data.pricing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CoinGeckoClient(private val httpClient: HttpClient) {

    suspend fun getPrices(coinIds: List<String>, vsCurrency: String = "usd"): Map<String, Double> {
        if (coinIds.isEmpty()) return emptyMap()
        return try {
            val response: JsonObject = httpClient.get(
                "https://api.coingecko.com/api/v3/simple/price"
            ) {
                parameter("ids", coinIds.joinToString(","))
                parameter("vs_currencies", vsCurrency)
            }.body()

            coinIds.mapNotNull { id ->
                val price = response[id]
                    ?.jsonObject?.get(vsCurrency)
                    ?.jsonPrimitive?.double
                if (price != null) id to price else null
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
