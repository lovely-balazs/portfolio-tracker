package app.portfoliotracker.data.repository

import app.portfoliotracker.data.database.PortfolioDatabase
import app.portfoliotracker.domain.model.Transaction
import app.portfoliotracker.domain.model.TransactionType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate

class TransactionRepository(private val db: PortfolioDatabase) {

    suspend fun findByImportHash(hash: String): Transaction? {
        return db.portfolioDatabaseQueries.selectTransactionByImportHash(hash)
            .executeAsOneOrNull()?.toDomain()
    }

    suspend fun getByInstrument(instrumentId: String): List<Transaction> {
        return db.portfolioDatabaseQueries.selectTransactionsByInstrument(instrumentId)
            .executeAsList().map { it.toDomain() }
    }

    suspend fun getAll(): List<Transaction> {
        return db.portfolioDatabaseQueries.selectAllTransactions()
            .executeAsList().map { it.toDomain() }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun insert(transaction: Transaction) {
        val now = Clock.System.now().toEpochMilliseconds()
        db.portfolioDatabaseQueries.insertTransaction(
            id = transaction.id,
            instrument_id = transaction.instrumentId,
            broker_source = transaction.brokerSource,
            type = transaction.type.name,
            date = transaction.date.toString(),
            quantity = transaction.quantity,
            price_per_unit = transaction.pricePerUnit,
            total_amount = transaction.totalAmount,
            currency = transaction.currency,
            fee = transaction.fee,
            fx_rate = transaction.fxRate,
            import_hash = transaction.importHash,
            notes = transaction.notes,
            created_at = now,
        )
    }

    suspend fun deleteByInstrument(instrumentId: String) {
        db.portfolioDatabaseQueries.deleteTransactionsByInstrument(instrumentId)
    }
}

private fun app.portfoliotracker.data.database.Txn.toDomain() = Transaction(
    id = id,
    instrumentId = instrument_id,
    brokerSource = broker_source,
    type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.BUY },
    date = LocalDate.parse(date),
    quantity = quantity,
    pricePerUnit = price_per_unit,
    totalAmount = total_amount,
    currency = currency,
    fee = fee,
    fxRate = fx_rate,
    importHash = import_hash,
    notes = notes,
)
