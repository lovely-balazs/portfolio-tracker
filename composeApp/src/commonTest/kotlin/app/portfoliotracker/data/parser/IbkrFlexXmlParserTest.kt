package app.portfoliotracker.data.parser

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IbkrFlexXmlParserTest {

    private val parser = IbkrFlexXmlParser()

    private val sampleXml = """
<?xml version="1.0" encoding="UTF-8"?>
<FlexQueryResponse queryName="Test" type="AF">
<FlexStatements count="1">
<FlexStatement accountId="U1234567">
<Trades>
<Trade symbol="AAPL" isin="US0378331005" description="APPLE INC" assetCategory="STK"
    dateTime="2025-01-15;09:30:00" quantity="100" tradePrice="150.50"
    ibCommission="-1.00" currency="USD" buySell="BUY" levelOfDetail="EXECUTION"
    fxRateToBase="0.92"/>
<Trade symbol="AAPL" isin="US0378331005" description="APPLE INC" assetCategory="STK"
    dateTime="2025-01-15;09:30:00" quantity="100" tradePrice="150.50"
    ibCommission="-1.00" currency="USD" buySell="BUY" levelOfDetail="ORDER"
    fxRateToBase="0.92"/>
<Trade symbol="MSFT" isin="US5949181045" description="MICROSOFT CORP" assetCategory="STK"
    dateTime="2025-01-20;14:00:00" quantity="50" tradePrice="380.00"
    ibCommission="-1.50" currency="USD" buySell="BUY" levelOfDetail="EXECUTION"
    fxRateToBase="0.93"/>
<Trade symbol="VWCE" isin="IE00BK5BQT80" description="VANGUARD FTSE ALL-WORLD" assetCategory="STK"
    dateTime="2025-02-01;10:15:00" quantity="-25" tradePrice="105.30"
    ibCommission="-0.50" currency="EUR" buySell="SELL" levelOfDetail="EXECUTION"/>
</Trades>
<CashTransactions>
<CashTransaction symbol="MSFT" isin="US5949181045" description="MSFT(US5949181045) Cash Dividend"
    dateTime="2025-03-15" amount="12.50" currency="USD" type="Dividends"
    fxRateToBase="0.91"/>
</CashTransactions>
</FlexStatement>
</FlexStatements>
</FlexQueryResponse>
    """.trimIndent()

    @Test
    fun canParseReturnsTrueForFlexXml() {
        assertTrue(parser.canParse(sampleXml))
    }

    @Test
    fun canParseReturnsFalseForCsv() {
        assertFalse(parser.canParse("Date,Type,Ticker,Name"))
    }

    @Test
    fun canParseReturnsFalseForIbkrCsv() {
        assertFalse(parser.canParse("Statement,Header,Field Name"))
    }

    @Test
    fun parsesThreeExecutionTradesSkipsOrder() {
        val result = parser.parse(sampleXml)
        val trades = result.transactions.filter { it.type == "BUY" || it.type == "SELL" }
        assertEquals(3, trades.size) // 2 BUY EXECUTION + 1 SELL EXECUTION, ORDER skipped
    }

    @Test
    fun parsesTradeFieldsCorrectly() {
        val result = parser.parse(sampleXml)
        val aapl = result.transactions.first { it.ticker == "AAPL" }
        assertEquals("ibkr-flex-xml", aapl.brokerSource)
        assertEquals(LocalDate(2025, 1, 15), aapl.date)
        assertEquals("BUY", aapl.type)
        assertEquals("US0378331005", aapl.isin)
        assertEquals(100.0, aapl.quantity)
        assertEquals(150.50, aapl.pricePerUnit)
        assertEquals(1.0, aapl.fee)
        assertEquals("USD", aapl.currency)
        assertEquals(0.92, aapl.fxRate)
    }

    @Test
    fun sellHasAbsoluteQuantity() {
        val result = parser.parse(sampleXml)
        val sell = result.transactions.first { it.type == "SELL" }
        assertEquals(25.0, sell.quantity) // original was -25
        assertEquals("VWCE", sell.ticker)
        assertEquals("EUR", sell.currency)
    }

    @Test
    fun parsesCashTransactionDividend() {
        val result = parser.parse(sampleXml)
        val div = result.transactions.first { it.type == "DIVIDEND" }
        assertEquals("MSFT", div.ticker)
        assertEquals(12.50, div.totalAmount)
        assertEquals("USD", div.currency)
        assertEquals(LocalDate(2025, 3, 15), div.date)
    }

    @Test
    fun multiCurrencyTradesRetainCurrency() {
        val result = parser.parse(sampleXml)
        val currencies = result.transactions
            .filter { it.type == "BUY" || it.type == "SELL" }
            .map { it.currency }
            .toSet()
        assertTrue(currencies.contains("USD"))
        assertTrue(currencies.contains("EUR"))
    }

    @Test
    fun noWarningsOnValidXml() {
        val result = parser.parse(sampleXml)
        assertEquals(0, result.warnings.size)
    }
}
