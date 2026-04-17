package app.portfoliotracker.data.pricing

import app.portfoliotracker.data.database.PortfolioDatabase
import app.portfoliotracker.domain.model.PriceSnapshot
import kotlinx.datetime.LocalDate

class PriceRepository(private val db: PortfolioDatabase) {

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun insertSnapshot(snapshot: PriceSnapshot) {
        db.portfolioDatabaseQueries.insertPriceSnapshot(
            instrument_id = snapshot.instrumentId,
            date = snapshot.date.toString(),
            price = snapshot.price,
            currency = snapshot.currency,
            source = snapshot.source,
            fetched_at = snapshot.fetchedAt,
        )
    }

    suspend fun getLatestPrice(instrumentId: String): PriceSnapshot? {
        return db.portfolioDatabaseQueries.selectLatestPrice(instrumentId)
            .executeAsOneOrNull()?.toDomain()
    }

    suspend fun getAllLatestPrices(): List<PriceSnapshot> {
        return db.portfolioDatabaseQueries.selectAllLatestPrices()
            .executeAsList().map { it.toDomain() }
    }

    suspend fun insertPortfolioSnapshot(date: LocalDate, totalValueBase: Double, baseCurrency: String) {
        db.portfolioDatabaseQueries.insertPortfolioSnapshot(
            date = date.toString(),
            total_value_base = totalValueBase,
            base_currency = baseCurrency,
        )
    }

    suspend fun getAllPortfolioSnapshots(): List<app.portfoliotracker.domain.model.PortfolioSnapshot> {
        return db.portfolioDatabaseQueries.selectAllPortfolioSnapshots()
            .executeAsList().map {
                app.portfoliotracker.domain.model.PortfolioSnapshot(
                    date = LocalDate.parse(it.date),
                    totalValueBase = it.total_value_base,
                    baseCurrency = it.base_currency,
                )
            }
    }
}

private fun app.portfoliotracker.data.database.Price_snapshot.toDomain() = PriceSnapshot(
    instrumentId = instrument_id,
    date = LocalDate.parse(date),
    price = price,
    currency = currency,
    source = source,
    fetchedAt = fetched_at,
)
