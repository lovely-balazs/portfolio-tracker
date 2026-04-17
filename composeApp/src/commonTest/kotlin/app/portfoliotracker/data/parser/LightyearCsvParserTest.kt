package app.portfoliotracker.data.parser

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LightyearCsvParserTest {

    private val parser = LightyearCsvParser()

    private val validCsv = """
Date,Type,Ticker,Name,Shares,Price per share,Total amount,Currency,FX rate,Total in EUR,Notes
15/01/2025,BUY,AAPL,Apple Inc.,10,150.50,1505.00,USD,0.92,1384.60,
20/01/2025,BUY,MSFT,Microsoft Corp.,5,380.00,1900.00,USD,0.93,1767.00,
01/02/2025,BUY,VWCE,"Vanguard FTSE All-World",20.5432,105.30,2164.74,EUR,,2164.74,
10/02/2025,SELL,AAPL,Apple Inc.,5,160.00,800.00,USD,0.91,728.00,
15/02/2025,DIVIDEND,MSFT,Microsoft Corp.,,,,USD,0.92,4.60,Quarterly dividend
    """.trimIndent()

    @Test
    fun canParseReturnsTrueForLightyearHeader() {
        assertTrue(parser.canParse(validCsv))
    }

    @Test
    fun canParseReturnsFalseForIbkrContent() {
        val ibkrContent = """
Statement,Header,Field Name,Field Value
Statement,Data,BrokerName,Interactive Brokers
Trades,Header,DataDiscriminator,Asset Category,Symbol
        """.trimIndent()
        assertFalse(parser.canParse(ibkrContent))
    }

    @Test
    fun canParseReturnsFalseForBinaryContent() {
        assertFalse(parser.canParse("\u0000\u0001\u0002binary"))
    }

    @Test
    fun parseFiveMixedTransactions() {
        val result = parser.parse(validCsv)
        assertEquals(5, result.transactions.size)
        assertEquals(0, result.skippedRows)
        assertEquals(0, result.warnings.size)
    }

    @Test
    fun parseBuyTransactionCorrectly() {
        val result = parser.parse(validCsv)
        val tx = result.transactions[0]
        assertEquals("lightyear-csv", tx.brokerSource)
        assertEquals(LocalDate(2025, 1, 15), tx.date)
        assertEquals("BUY", tx.type)
        assertEquals("AAPL", tx.ticker)
        assertEquals("Apple Inc.", tx.name)
        assertEquals(10.0, tx.quantity)
        assertEquals(150.50, tx.pricePerUnit)
        assertEquals(1505.0, tx.totalAmount)
        assertEquals("USD", tx.currency)
        assertEquals(0.92, tx.fxRate)
    }

    @Test
    fun parseSellTransaction() {
        val result = parser.parse(validCsv)
        val sell = result.transactions[3]
        assertEquals("SELL", sell.type)
        assertEquals("AAPL", sell.ticker)
        assertEquals(5.0, sell.quantity)
    }

    @Test
    fun parseDividendTransaction() {
        val result = parser.parse(validCsv)
        val div = result.transactions[4]
        assertEquals("DIVIDEND", div.type)
        assertEquals("MSFT", div.ticker)
        assertEquals("Quarterly dividend", div.notes)
    }

    @Test
    fun parseFractionalShares() {
        val result = parser.parse(validCsv)
        val tx = result.transactions[2]
        assertEquals(20.5432, tx.quantity)
        assertEquals("VWCE", tx.ticker)
    }

    @Test
    fun parseDepositWithNullTicker() {
        val csv = """
Date,Type,Ticker,Name,Shares,Price per share,Total amount,Currency,FX rate,Total in EUR,Notes
05/03/2025,DEPOSIT,,,,,500.00,EUR,,500.00,Bank transfer
        """.trimIndent()
        val result = parser.parse(csv)
        assertEquals(1, result.transactions.size)
        val tx = result.transactions[0]
        assertEquals("DEPOSIT", tx.type)
        assertEquals(null, tx.ticker)
        assertEquals(null, tx.quantity)
        assertEquals(500.0, tx.totalAmount)
    }

    @Test
    fun parseCustodyFeeMapToFee() {
        val csv = """
Date,Type,Ticker,Name,Shares,Price per share,Total amount,Currency,FX rate,Total in EUR,Notes
01/04/2025,CUSTODY_FEE,,,,,1.50,EUR,,1.50,
        """.trimIndent()
        val result = parser.parse(csv)
        assertEquals("FEE", result.transactions[0].type)
    }

    @Test
    fun parseHeaderOnlyReturnsEmpty() {
        val csv = "Date,Type,Ticker,Name,Shares,Price per share,Total amount,Currency,FX rate,Total in EUR,Notes"
        val result = parser.parse(csv)
        assertEquals(0, result.transactions.size)
        assertEquals(0, result.warnings.size)
        assertEquals(0, result.skippedRows)
    }

    @Test
    fun malformedDateProducesWarning() {
        val csv = """
Date,Type,Ticker,Name,Shares,Price per share,Total amount,Currency,FX rate,Total in EUR,Notes
31/13/2025,BUY,AAPL,Apple Inc.,10,150.50,1505.00,USD,,,
15/01/2025,BUY,MSFT,Microsoft Corp.,5,380.00,1900.00,USD,,,
        """.trimIndent()
        val result = parser.parse(csv)
        assertEquals(1, result.transactions.size) // second row still parsed
        assertEquals(1, result.warnings.size)
        assertEquals(1, result.skippedRows)
        assertEquals("MSFT", result.transactions[0].ticker)
    }

    @Test
    fun quotedFieldWithComma() {
        val csv = """
Date,Type,Ticker,Name,Shares,Price per share,Total amount,Currency,FX rate,Total in EUR,Notes
15/01/2025,BUY,VOW3,"Volkswagen AG, Vz.",10,120.00,1200.00,EUR,,1200.00,
        """.trimIndent()
        val result = parser.parse(csv)
        assertEquals(1, result.transactions.size)
        assertEquals("Volkswagen AG, Vz.", result.transactions[0].name)
    }
}
