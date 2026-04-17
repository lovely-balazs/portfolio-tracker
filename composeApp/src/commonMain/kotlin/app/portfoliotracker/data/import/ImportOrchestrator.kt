package app.portfoliotracker.data.import

import app.portfoliotracker.data.parser.BrokerParser
import app.portfoliotracker.data.parser.ImportedTransaction
import app.portfoliotracker.data.parser.ParseResult
import app.portfoliotracker.data.repository.InstrumentRepository
import app.portfoliotracker.data.repository.TransactionRepository
import app.portfoliotracker.data.repository.generateUuid
import app.portfoliotracker.domain.model.Transaction
import app.portfoliotracker.domain.model.TransactionType

class ImportOrchestrator(
    private val parsers: List<BrokerParser>,
    private val instrumentRepo: InstrumentRepository,
    private val transactionRepo: TransactionRepository,
) {
    fun import(content: String): ImportResult {
        // 1. Detect format
        val parser = parsers.firstOrNull { it.canParse(content) }
            ?: return ImportResult(
                imported = 0,
                skipped = 0,
                duplicates = 0,
                warnings = listOf("Unrecognized file format"),
            )

        // 2. Parse
        val parseResult = parser.parse(content)

        // 3. Map, resolve instruments, dedup, persist
        var imported = 0
        var duplicates = 0
        val warnings = parseResult.warnings.toMutableList()

        for (importedTx in parseResult.transactions) {
            try {
                val hash = computeHash(importedTx)

                // Dedup check
                if (transactionRepo.findByImportHash(hash) != null) {
                    duplicates++
                    continue
                }

                // Resolve instrument
                val instrument = instrumentRepo.resolveOrCreate(
                    isin = importedTx.isin,
                    ticker = importedTx.ticker,
                    name = importedTx.name,
                    currency = importedTx.currency,
                )

                // Map to domain
                val transaction = Transaction(
                    id = generateUuid(),
                    instrumentId = instrument.id,
                    brokerSource = importedTx.brokerSource,
                    type = mapType(importedTx.type),
                    date = importedTx.date,
                    quantity = importedTx.quantity ?: 0.0,
                    pricePerUnit = importedTx.pricePerUnit ?: 0.0,
                    totalAmount = importedTx.totalAmount ?: 0.0,
                    currency = importedTx.currency,
                    fee = importedTx.fee,
                    fxRate = importedTx.fxRate,
                    importHash = hash,
                    notes = importedTx.notes,
                )

                transactionRepo.insert(transaction)
                imported++
            } catch (e: Exception) {
                warnings.add("Failed to import: ${e.message}")
            }
        }

        return ImportResult(
            imported = imported,
            skipped = parseResult.skippedRows,
            duplicates = duplicates,
            warnings = warnings,
        )
    }

    private fun mapType(type: String): TransactionType = when (type.uppercase()) {
        "BUY" -> TransactionType.BUY
        "SELL" -> TransactionType.SELL
        "DIVIDEND" -> TransactionType.DIVIDEND
        "FEE" -> TransactionType.FEE
        "DEPOSIT" -> TransactionType.DEPOSIT
        "WITHDRAWAL" -> TransactionType.WITHDRAWAL
        "INTEREST" -> TransactionType.INTEREST
        "CORPORATE_ACTION" -> TransactionType.CORPORATE_ACTION
        else -> TransactionType.BUY
    }

    companion object {
        fun computeHash(tx: ImportedTransaction): String {
            val raw = "${tx.brokerSource}|${tx.date}|${tx.ticker}|${tx.quantity}|${tx.pricePerUnit}|${tx.totalAmount}"
            // Simple string hash — deterministic across platforms
            var hash = 0L
            for (ch in raw) {
                hash = 31 * hash + ch.code
            }
            return hash.toString(16)
        }
    }
}

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val duplicates: Int,
    val warnings: List<String>,
)
