package app.portfoliotracker.data.pricing

import app.portfoliotracker.domain.model.AssetClass
import app.portfoliotracker.domain.model.Instrument
import app.portfoliotracker.domain.model.PriceSnapshot
import kotlinx.datetime.LocalDate

class PriceService(
    private val finnhubClient: FinnhubClient,
    private val yahooClient: YahooFinanceClient,
    private val coinGeckoClient: CoinGeckoClient,
    private val priceRepo: PriceRepository,
) {
    suspend fun refreshPrices(instruments: List<Instrument>): PriceRefreshResult {
        val warnings = mutableListOf<String>()
        var updated = 0

        @OptIn(kotlin.time.ExperimentalTime::class)
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val today = now.toString().take(10) // rough — will be overridden

        val todayDate = currentDate()

        // Group by asset class
        val equities = instruments.filter {
            it.assetClass in listOf(AssetClass.STOCK, AssetClass.ETF, AssetClass.BOND, AssetClass.MUTUAL_FUND)
        }
        val crypto = instruments.filter { it.assetClass == AssetClass.CRYPTO }

        // Fetch equity prices via Finnhub, fallback to Yahoo
        for (inst in equities) {
            val price = finnhubClient.getQuote(inst.ticker)
                ?: yahooClient.getQuote(inst.ticker)
            if (price != null) {
                priceRepo.insertSnapshot(
                    PriceSnapshot(
                        instrumentId = inst.id,
                        date = todayDate,
                        price = price,
                        currency = inst.currency,
                        source = "finnhub/yahoo",
                        fetchedAt = now,
                    )
                )
                updated++
            } else {
                warnings.add("No price for ${inst.ticker}")
            }
        }

        // Fetch crypto prices via CoinGecko
        if (crypto.isNotEmpty()) {
            val coinIds = crypto.map { it.ticker.lowercase() }
            val prices = coinGeckoClient.getPrices(coinIds)
            for (inst in crypto) {
                val price = prices[inst.ticker.lowercase()]
                if (price != null) {
                    priceRepo.insertSnapshot(
                        PriceSnapshot(
                            instrumentId = inst.id,
                            date = todayDate,
                            price = price,
                            currency = "USD",
                            source = "coingecko",
                            fetchedAt = now,
                        )
                    )
                    updated++
                } else {
                    warnings.add("No price for ${inst.ticker}")
                }
            }
        }

        return PriceRefreshResult(updated = updated, warnings = warnings)
    }

    private fun currentDate(): LocalDate {
        @OptIn(kotlin.time.ExperimentalTime::class)
        val now = kotlin.time.Clock.System.now()
        val dateStr = now.toString().take(10)
        return LocalDate.parse(dateStr)
    }
}

data class PriceRefreshResult(
    val updated: Int,
    val warnings: List<String>,
)
