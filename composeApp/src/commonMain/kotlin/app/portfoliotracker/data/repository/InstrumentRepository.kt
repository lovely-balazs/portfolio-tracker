package app.portfoliotracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.portfoliotracker.data.database.PortfolioDatabase
import app.portfoliotracker.domain.model.AssetClass
import app.portfoliotracker.domain.model.Instrument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class InstrumentRepository(private val db: PortfolioDatabase) {

    suspend fun findByIsin(isin: String): Instrument? {
        return db.portfolioDatabaseQueries.selectInstrumentByIsin(isin)
            .asFlow().mapToOneOrNull(Dispatchers.Default).first()?.toDomain()
    }

    suspend fun findByTickerAndCurrency(ticker: String, currency: String): Instrument? {
        return db.portfolioDatabaseQueries.selectInstrumentByTickerAndCurrency(ticker, currency)
            .asFlow().mapToOneOrNull(Dispatchers.Default).first()?.toDomain()
    }

    suspend fun findById(id: String): Instrument? {
        return db.portfolioDatabaseQueries.selectInstrumentById(id)
            .asFlow().mapToOneOrNull(Dispatchers.Default).first()?.toDomain()
    }

    suspend fun getAll(): List<Instrument> {
        return db.portfolioDatabaseQueries.selectAllInstruments()
            .asFlow().mapToList(Dispatchers.Default).first().map { it.toDomain() }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun insert(instrument: Instrument) {
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

    suspend fun resolveOrCreate(
        isin: String?,
        ticker: String?,
        name: String?,
        currency: String,
        assetClass: AssetClass = AssetClass.STOCK,
    ): Instrument {
        if (isin != null) {
            findByIsin(isin)?.let { return it }
        }
        if (ticker != null) {
            findByTickerAndCurrency(ticker, currency)?.let { return it }
        }

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
