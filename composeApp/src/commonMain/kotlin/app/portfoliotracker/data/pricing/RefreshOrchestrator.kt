package app.portfoliotracker.data.pricing

import app.portfoliotracker.data.repository.InstrumentRepository
import app.portfoliotracker.data.repository.TransactionRepository
import app.portfoliotracker.domain.model.Holding
import kotlinx.datetime.LocalDate

class RefreshOrchestrator(
    private val priceService: PriceService,
    private val fxRateService: FxRateService,
    private val priceRepo: PriceRepository,
    private val instrumentRepo: InstrumentRepository,
    private val transactionRepo: TransactionRepository,
) {
    suspend fun refresh(baseCurrency: String): RefreshResult {
        val instruments = instrumentRepo.getAll()
        val warnings = mutableListOf<String>()

        // 1. Refresh prices
        val priceResult = priceService.refreshPrices(instruments)
        warnings.addAll(priceResult.warnings)

        // 2. Fetch FX rates
        val fxRates = fxRateService.getRates(baseCurrency)

        // 3. Compute total portfolio value in base currency
        val latestPrices = priceRepo.getAllLatestPrices()
        val priceMap = latestPrices.associate { it.instrumentId to it }

        var totalValueBase = 0.0
        for (inst in instruments) {
            val txns = transactionRepo.getByInstrument(inst.id)
            val price = priceMap[inst.id]?.price
            val holding = Holding.fromTransactions(inst, txns, price) ?: continue
            val value = holding.currentValue ?: continue
            val valueBase = fxRateService.convertToBase(value, inst.currency, fxRates)
            totalValueBase += valueBase
        }

        // 4. Record portfolio snapshot
        val today = currentDate()
        priceRepo.insertPortfolioSnapshot(today, totalValueBase, baseCurrency)

        return RefreshResult(
            pricesUpdated = priceResult.updated,
            totalValueBase = totalValueBase,
            baseCurrency = baseCurrency,
            warnings = warnings,
        )
    }

    private fun currentDate(): LocalDate {
        @OptIn(kotlin.time.ExperimentalTime::class)
        val now = kotlin.time.Clock.System.now()
        val dateStr = now.toString().take(10)
        return LocalDate.parse(dateStr)
    }
}

data class RefreshResult(
    val pricesUpdated: Int,
    val totalValueBase: Double,
    val baseCurrency: String,
    val warnings: List<String>,
)
