package app.portfoliotracker.domain.model

data class Instrument(
    val id: String,
    val isin: String?,
    val ticker: String,
    val name: String,
    val assetClass: AssetClass,
    val currency: String,
    val exchange: String?,
)
