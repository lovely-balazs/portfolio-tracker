package app.portfoliotracker.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HoldingTest {

    private val instrument = Instrument(
        id = "inst-1",
        isin = "US0378331005",
        ticker = "AAPL",
        name = "Apple Inc.",
        assetClass = AssetClass.STOCK,
        currency = "USD",
        exchange = "NASDAQ",
    )

    private fun buyTx(qty: Double, price: Double, date: String = "2025-01-01") = Transaction(
        id = "tx-buy-$qty-$price",
        instrumentId = instrument.id,
        brokerSource = "test",
        type = TransactionType.BUY,
        date = LocalDate.parse(date),
        quantity = qty,
        pricePerUnit = price,
        totalAmount = qty * price,
        currency = "USD",
        fee = 0.0,
        fxRate = null,
        importHash = null,
        notes = null,
    )

    private fun sellTx(qty: Double, price: Double, date: String = "2025-06-01") = Transaction(
        id = "tx-sell-$qty-$price",
        instrumentId = instrument.id,
        brokerSource = "test",
        type = TransactionType.SELL,
        date = LocalDate.parse(date),
        quantity = qty,
        pricePerUnit = price,
        totalAmount = qty * price,
        currency = "USD",
        fee = 0.0,
        fxRate = null,
        importHash = null,
        notes = null,
    )

    @Test
    fun emptyTransactionsReturnsNull() {
        val holding = Holding.fromTransactions(instrument, emptyList())
        assertNull(holding)
    }

    @Test
    fun threeBuyLotsAggregateCorrectly() {
        val txns = listOf(
            buyTx(100.0, 10.0, "2025-01-01"),
            buyTx(50.0, 12.0, "2025-02-01"),
            buyTx(25.0, 8.0, "2025-03-01"),
        )
        val holding = Holding.fromTransactions(instrument, txns, currentPrice = 11.0)
        assertNotNull(holding)
        assertEquals(175.0, holding.totalQuantity)
        assertEquals(1800.0, holding.totalCost) // 1000 + 600 + 200
        assertEquals(1925.0, holding.currentValue) // 175 * 11
        assertEquals(125.0, holding.gainLoss) // 1925 - 1800
    }

    @Test
    fun buyThenPartialSellReducesCostBasisProportionally() {
        val txns = listOf(
            buyTx(100.0, 10.0, "2025-01-01"),
            sellTx(50.0, 15.0, "2025-06-01"),
        )
        val holding = Holding.fromTransactions(instrument, txns, currentPrice = 12.0)
        assertNotNull(holding)
        assertEquals(50.0, holding.totalQuantity)
        assertEquals(500.0, holding.totalCost) // 1000 * (50/100) = 500 remaining
        assertEquals(600.0, holding.currentValue) // 50 * 12
        assertEquals(100.0, holding.gainLoss) // 600 - 500
    }

    @Test
    fun gainLossPercentCalculation() {
        val txns = listOf(buyTx(100.0, 10.0))
        val holding = Holding.fromTransactions(instrument, txns, currentPrice = 12.0)
        assertNotNull(holding)
        assertEquals(200.0, holding.gainLoss) // 1200 - 1000
        assertEquals(20.0, holding.gainLossPercent) // 200/1000 * 100
    }

    @Test
    fun noPriceReturnsNullValues() {
        val txns = listOf(buyTx(100.0, 10.0))
        val holding = Holding.fromTransactions(instrument, txns, currentPrice = null)
        assertNotNull(holding)
        assertNull(holding.currentValue)
        assertNull(holding.gainLoss)
        assertNull(holding.gainLossPercent)
    }

    @Test
    fun sellOnlyResultsInNegativeQuantity() {
        val txns = listOf(sellTx(50.0, 15.0))
        val holding = Holding.fromTransactions(instrument, txns)
        assertNotNull(holding)
        assertEquals(-50.0, holding.totalQuantity)
    }

    @Test
    fun feesIncludedInCostBasis() {
        val tx = Transaction(
            id = "tx-fee",
            instrumentId = instrument.id,
            brokerSource = "test",
            type = TransactionType.BUY,
            date = LocalDate.parse("2025-01-01"),
            quantity = 100.0,
            pricePerUnit = 10.0,
            totalAmount = 1000.0,
            currency = "USD",
            fee = 5.0,
            fxRate = null,
            importHash = null,
            notes = null,
        )
        val holding = Holding.fromTransactions(instrument, listOf(tx), currentPrice = 10.0)
        assertNotNull(holding)
        assertEquals(1005.0, holding.totalCost) // includes fee
    }
}
