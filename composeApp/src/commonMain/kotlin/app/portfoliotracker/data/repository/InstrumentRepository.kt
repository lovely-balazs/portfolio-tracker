package app.portfoliotracker.data.repository

import app.portfoliotracker.data.database.PortfolioDatabase
import app.portfoliotracker.domain.model.AssetClass
import app.portfoliotracker.domain.model.Instrument
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class InstrumentRepository(private val db: PortfolioDatabase) {

    fun findByIsin(isin: String): Instrument? {
        return db.portfolioDatabaseQueries.selectInstrumentByIsin(isin)
            .executeAsOneOrNull()?.toDomain()
    }

    fun findByTickerAndCurrency(ticker: String, currency: String): Instrument? {
        return db.portfolioDatabaseQueries.selectInstrumentByTickerAndCurrency(ticker, currency)
            .executeAsOneOrNull()?.toDomain()
    }

    fun findById(id: String): Instrument? {
        return db.portfolioDatabaseQueries.selectInstrumentById(id)
            .executeAsOneOrNull()?.toDomain()
    }

    fun getAll(): List<Instrument> {
        return db.portfolioDatabaseQueries.selectAllInstruments()
            .executeAsList().map { it.toDomain() }
    }

    @OptIn(ExperimentalTime::class)
    fun insert(instrument: Instrument) {
        val now = Clock.System.now().toEpochMilliseconds()
        db.portfolioDatabaseQueries.insertInstrument(
            id = instrument.id,
            isin = instrument.isin,
            ticker = instrument.ticker,
            name = instrument.name,
            asset_class = instrument.assetClass.name,
            currency = instrument.currency,
            exchange = instrument.exchange,
            created_at = now,
            updated_at = now,
        )
    }

    fun resolveOrCreate(
        isin: String?,
        ticker: String?,
        name: String?,
        currency: String,
        assetClass: AssetClass = AssetClass.STOCK,
    ): Instrument {
        // Try ISIN match first
        if (isin != null) {
            findByIsin(isin)?.let { return it }
        }
        // Try ticker + currency match
        if (ticker != null) {
            findByTickerAndCurrency(ticker, currency)?.let { return it }
        }

        // Create new
        val instrument = Instrument(
            id = generateUuid(),
            isin = isin,
            ticker = ticker ?: "UNKNOWN",
            name = name ?: ticker ?: "Unknown",
            assetClass = assetClass,
            currency = currency,
            exchange = null,
        )
        insert(instrument)
        return instrument
    }
}

private fun app.portfoliotracker.data.database.Instrument.toDomain() = Instrument(
    id = id,
    isin = isin,
    ticker = ticker,
    name = name,
    assetClass = try { AssetClass.valueOf(asset_class) } catch (_: Exception) { AssetClass.OTHER },
    currency = currency,
    exchange = exchange,
)

internal expect fun generateUuid(): String
