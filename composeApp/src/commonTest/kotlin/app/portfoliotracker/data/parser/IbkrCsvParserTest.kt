package app.portfoliotracker.data.parser

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IbkrCsvParserTest {

    private val parser = IbkrCsvParser()

    private val sampleCsv = """
Statement,Header,Field Name,Field Value
Statement,Data,BrokerName,Interactive Brokers
Trades,Header,DataDiscriminator,Asset Category,Currency,Symbol,Date/Time,Quantity,T. Price,Comm/Fee,Code
Trades,Data,Trade,Stocks,USD,AAPL,"2025-01-15, 09:30:00",100,150.50,-1.00,
Trades,Data,Trade,Stocks,USD,MSFT,"2025-01-20, 14:00:00",50,380.00,-1.50,
Trades,Data,Trade,Stocks,EUR,VWCE,"2025-02-01, 10:15:00",-25,105.30,-0.50,
Trades,Data,ClosedLot,Stocks,USD,AAPL,"2025-01-15, 09:30:00",100,150.50,-1.00,
Trades,Total,,,,,,,,
Dividends,Header,Currency,Date,Description,Amount
Dividends,Data,USD,2025-03-15,"AAPL(US0378331005) Cash Dividend 0.25 per Share",25.00
Dividends,Total,,,
Financial Instrument Information,Header,Asset Category,Symbol,Description,Security ID,Security ID Type
Financial Instrument Information,Data,Stocks,AAPL,APPLE INC,US0378331005,ISIN
Financial Instrument Information,Data,Stocks,MSFT,MICROSOFT CORP,US5949181045,ISIN
Financial Instrument Information,Data,Stocks,VWCE,VANGUARD FTSE ALL-WORLD,IE00BK5BQT80,ISIN
    """.trimIndent()

    @Test
    fun canParseReturnsTrueForIbkrCsv() {
        assertTrue(parser.canParse(sampleCsv))
    }

    @Test
    fun canParseReturnsFalseForLightyear() {
        assertFalse(parser.canParse("Date,Type,Ticker,Name,Shares,Price per share"))
    }

    @Test
    fun canParseReturnsFalseForXml() {
        assertFalse(parser.canParse("<?xml version=\"1.0\"?><FlexQueryResponse>"))
    }

    @Test
    fun parsesTradesFromMultiSectionCsv() {
        val result = parser.parse(sampleCsv)
        val trades = result.transactions.filter { it.type == "BUY" || it.type == "SELL" }
        assertEquals(3, trades.size) // 2 BUY + 1 SELL, ClosedLot skipped
    }

    @Test
    fun closedLotRowsSkipped() {
        val result = parser.parse(sampleCsv)
        // ClosedLot row should not produce a transaction
        val trades = result.transactions.filter { it.type == "BUY" || it.type == "SELL" }
        assertEquals(3, trades.size)
    }

    @Test
    fun embeddedCommaInDateTimeParsedCorrectly() {
        val result = parser.parse(sampleCsv)
        val aapl = result.transactions.first { it.ticker == "AAPL" && it.type == "BUY" }
        assertEquals(LocalDate(2025, 1, 15), aapl.date)
    }

    @Test
    fun sellDetectedByNegativeQuantity() {
        val result = parser.parse(sampleCsv)
        val sell = result.transactions.first { it.type == "SELL" }
        assertEquals("VWCE", sell.ticker)
        assertEquals(25.0, sell.quantity) // absolute value
        assertEquals("EUR", sell.currency)
    }

    @Test
    fun dividendsParsed() {
        val result = parser.parse(sampleCsv)
        val divs = result.transactions.filter { it.type == "DIVIDEND" }
        assertEquals(1, divs.size)
        assertEquals("AAPL", divs[0].ticker)
        assertEquals(25.0, divs[0].totalAmount)
    }

    @Test
    fun instrumentInfoProvidesIsinToTrades() {
        val result = parser.parse(sampleCsv)
        val aapl = result.transactions.first { it.ticker == "AAPL" && it.type == "BUY" }
        assertEquals("US0378331005", aapl.isin)
        val msft = result.transactions.first { it.ticker == "MSFT" }
        assertEquals("US5949181045", msft.isin)
    }

    @Test
    fun emptyDataSectionNoErrors() {
        val csv = """
Statement,Header,Field Name,Field Value
Statement,Data,BrokerName,Interactive Brokers
Trades,Header,DataDiscriminator,Asset Category,Currency,Symbol,Date/Time,Quantity,T. Price,Comm/Fee,Code
Trades,Total,,,,,,,,
        """.trimIndent()
        val result = parser.parse(csv)
        assertEquals(0, result.transactions.size)
        assertEquals(0, result.warnings.size)
    }

    @Test
    fun noWarningsOnValidCsv() {
        val result = parser.parse(sampleCsv)
        assertEquals(0, result.warnings.size)
    }

    @Test
    fun tradeFieldsCorrect() {
        val result = parser.parse(sampleCsv)
        val aapl = result.transactions.first { it.ticker == "AAPL" && it.type == "BUY" }
        assertEquals("ibkr-csv", aapl.brokerSource)
        assertEquals(100.0, aapl.quantity)
        assertEquals(150.50, aapl.pricePerUnit)
        assertEquals(1.0, aapl.fee)
        assertEquals("USD", aapl.currency)
    }
}
