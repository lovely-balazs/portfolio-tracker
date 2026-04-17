package app.portfoliotracker.data.import

import app.portfoliotracker.data.parser.BrokerParser
import app.portfoliotracker.data.parser.ImportedTransaction
import app.portfoliotracker.data.parser.ParseResult
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ImportOrchestratorTest {

    @Test
    fun computeHashIsDeterministic() {
        val tx = sampleImportedTx()
        val hash1 = ImportOrchestrator.computeHash(tx)
        val hash2 = ImportOrchestrator.computeHash(tx)
        assertEquals(hash1, hash2)
    }

    @Test
    fun computeHashDiffersForDifferentTransactions() {
        val tx1 = sampleImportedTx(ticker = "AAPL")
        val tx2 = sampleImportedTx(ticker = "MSFT")
        assertNotEquals(
            ImportOrchestrator.computeHash(tx1),
            ImportOrchestrator.computeHash(tx2),
        )
    }

    @Test
    fun computeHashDiffersForDifferentDates() {
        val tx1 = sampleImportedTx(date = LocalDate(2025, 1, 1))
        val tx2 = sampleImportedTx(date = LocalDate(2025, 1, 2))
        assertNotEquals(
            ImportOrchestrator.computeHash(tx1),
            ImportOrchestrator.computeHash(tx2),
        )
    }

    @Test
    fun computeHashIncludesBrokerSource() {
        val tx1 = sampleImportedTx(brokerSource = "lightyear-csv")
        val tx2 = sampleImportedTx(brokerSource = "ibkr-flex-xml")
        assertNotEquals(
            ImportOrchestrator.computeHash(tx1),
            ImportOrchestrator.computeHash(tx2),
        )
    }

    @Test
    fun formatDetectionSelectsCorrectParser() {
        val parser1 = FakeParser("fmt1", canParse = false)
        val parser2 = FakeParser("fmt2", canParse = true)
        val parser3 = FakeParser("fmt3", canParse = true)

        // Should pick first parser that returns canParse=true
        val parsers = listOf(parser1, parser2, parser3)
        val match = parsers.firstOrNull { it.canParse("test") }
        assertEquals("fmt2", match?.formatId)
    }

    @Test
    fun noMatchingParserIdentified() {
        val parser1 = FakeParser("fmt1", canParse = false)
        val parser2 = FakeParser("fmt2", canParse = false)
        val parsers = listOf(parser1, parser2)
        val match = parsers.firstOrNull { it.canParse("test") }
        assertEquals(null, match)
    }

    private fun sampleImportedTx(
        brokerSource: String = "test",
        ticker: String = "AAPL",
        date: LocalDate = LocalDate(2025, 1, 15),
    ) = ImportedTransaction(
        brokerSource = brokerSource,
        date = date,
        type = "BUY",
        ticker = ticker,
        isin = null,
        name = "Apple Inc.",
        quantity = 100.0,
        pricePerUnit = 150.0,
        totalAmount = 15000.0,
        currency = "USD",
        fee = 0.0,
        fxRate = null,
        notes = null,
    )

    private class FakeParser(
        override val formatId: String,
        private val canParse: Boolean,
    ) : BrokerParser {
        override fun canParse(content: String) = canParse
        override fun parse(content: String) = ParseResult(emptyList(), emptyList(), 0)
    }
}
