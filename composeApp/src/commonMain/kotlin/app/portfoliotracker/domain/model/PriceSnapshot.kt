package app.portfoliotracker.domain.model

import kotlinx.datetime.LocalDate

data class PriceSnapshot(
    val instrumentId: String,
    val date: LocalDate,
    val price: Double,
    val currency: String,
    val source: String,
    val fetchedAt: Long,
)
