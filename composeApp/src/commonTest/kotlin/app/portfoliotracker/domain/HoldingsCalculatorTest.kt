package app.portfoliotracker.domain

import app.portfoliotracker.data.pricing.FxRates
import app.portfoliotracker.domain.model.AssetClass
import app.portfoliotracker.domain.model.Instrument
import app.portfoliotracker.domain.model.PriceSnapshot
import app.portfoliotracker.domain.model.Transaction
import app.portfoliotracker.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HoldingsCalculatorTest {

    private val aapl = Instrument("i1", "US0378331005", "AAPL", "Apple Inc.", AssetClass.STOCK, "USD", "NASDAQ")
    private val vwce = Instrument("i2", "IE00BK5BQT80", "VWCE", "Vanguard FTSE All-World", AssetClass.ETF, "EUR", "XETRA")
    private val btc = Instrument("i3", null, "bitcoin", "Bitcoin", AssetClass.CRYPTO, "USD", null)

    private val fxRates = FxRates(
        base = "EUR",
        rates = mapOf("USD" to 1.08, "GBP" to 0.86),
    )

    @Test
    fun multiCurrencyPortfolioTotalInBaseCurrency() {
        val txns = mapOf(
            "i1" to listOf(buyTx("i1", 10.0, 150.0, "USD")),
            "i2" to listOf(buyTx("i2", 20.0, 100.0, "EUR")),
        )
        val prices = mapOf(
            "i1" to priceSnapshot("i1", 160.0, "USD"),
            "i2" to priceSnapshot("i2", 110.0, "EUR"),
        )

        val summary = HoldingsCalculator.calculate(
            instruments = listOf(aapl, vwce),
            transactionsByInstrument = txns,
            latestPrices = prices,
            fxRates = fxRates,
            baseCurrency = "EUR",
        )

        // AAPL: 10 * 160 = 1600 USD → 1600/1.08 ≈ 1481.48 EUR
        // VWCE: 20 * 110 = 2200 EUR
        // Total ≈ 3681.48 EUR
        assertTrue(abs(summary.totalValueBase - 3681.48) < 1.0)
        assertEquals(2, summary.holdings.size)
    }

    @Test
    fun allocationsSumToOneHundred() {
        val txns = mapOf(
            "i1" to listOf(buyTx("i1", 10.0, 150.0, "USD")),
            "i2" to listOf(buyTx("i2", 20.0, 100.0, "EUR")),
            "i3" to listOf(buyTx("i3", 0.5, 30000.0, "USD")),
        )
        val prices = mapOf(
            "i1" to priceSnapshot("i1", 160.0, "USD"),
            "i2" to priceSnapshot("i2", 110.0, "EUR"),
            "i3" to priceSnapshot("i3", 35000.0, "USD"),
        )

        val summary = HoldingsCalculator.calculate(
            instruments = listOf(aapl, vwce, btc),
            transactionsByInstrument = txns,
            latestPrices = prices,
            fxRates = fxRates,
            baseCurrency = "EUR",
        )

        val totalAlloc = summary.holdings.sumOf { it.allocationPercent }
        assertTrue(abs(totalAlloc - 100.0) < 0.1)
    }

    @Test
    fun singleHoldingIsOneHundredPercent() {
        val txns = mapOf("i1" to listOf(buyTx("i1", 10.0, 150.0, "USD")))
        val prices = mapOf("i1" to priceSnapshot("i1", 160.0, "USD"))

        val summary = HoldingsCalculator.calculate(
            instruments = listOf(aapl),
            transactionsByInstrument = txns,
            latestPrices = prices,
            fxRates = fxRates,
            baseCurrency = "EUR",
        )

        assertEquals(1, summary.holdings.size)
        assertTrue(abs(summary.holdings[0].allocationPercent - 100.0) < 0.01)
    }

    @Test
    fun noTransactionsReturnsEmptySummary() {
        val summary = HoldingsCalculator.calculate(
            instruments = listOf(aapl),
            transactionsByInstrument = emptyMap(),
            latestPrices = emptyMap(),
            fxRates = fxRates,
            baseCurrency = "EUR",
        )

        assertEquals(0, summary.holdings.size)
        assertEquals(0.0, summary.totalValueBase)
    }

    @Test
    fun fxConversionUsdToEur() {
        val txns = mapOf("i1" to listOf(buyTx("i1", 10.0, 100.0, "USD")))
        val prices = mapOf("i1" to priceSnapshot("i1", 100.0, "USD"))

        val summary = HoldingsCalculator.calculate(
            instruments = listOf(aapl),
            transactionsByInstrument = txns,
            latestPrices = prices,
            fxRates = fxRates,
            baseCurrency = "EUR",
        )

        // 1000 USD / 1.08 ≈ 925.93 EUR
        assertTrue(abs(summary.totalValueBase - 925.93) < 1.0)
    }

    @Test
    fun holdingsOrderedByValueDescending() {
        val txns = mapOf(
            "i1" to listOf(buyTx("i1", 1.0, 100.0, "USD")),
            "i2" to listOf(buyTx("i2", 100.0, 100.0, "EUR")),
        )
        val prices = mapOf(
            "i1" to priceSnapshot("i1", 100.0, "USD"),
            "i2" to priceSnapshot("i2", 100.0, "EUR"),
        )

        val summary = HoldingsCalculator.calculate(
            instruments = listOf(aapl, vwce),
            transactionsByInstrument = txns,
            latestPrices = prices,
            fxRates = fxRates,
            baseCurrency = "EUR",
        )

        // VWCE (10000 EUR) should be first, AAPL (100 USD ≈ 92 EUR) second
        assertEquals("VWCE", summary.holdings[0].holding.instrument.ticker)
    }

    private fun buyTx(instrumentId: String, qty: Double, price: Double, currency: String) = Transaction(
        id = "tx-$instrumentId-$qty",
        instrumentId = instrumentId,
        brokerSource = "test",
        type = TransactionType.BUY,
        date = LocalDate(2025, 1, 1),
        quantity = qty,
        pricePerUnit = price,
        totalAmount = qty * price,
        currency = currency,
        fee = 0.0,
        fxRate = null,
        importHash = null,
        notes = null,
    )

    private fun priceSnapshot(instrumentId: String, price: Double, currency: String) = PriceSnapshot(
        instrumentId = instrumentId,
        date = LocalDate(2025, 6, 1),
        price = price,
        currency = currency,
        source = "test",
        fetchedAt = 0L,
    )
}
