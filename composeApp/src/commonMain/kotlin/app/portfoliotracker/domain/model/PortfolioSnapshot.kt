package app.portfoliotracker.domain.model

import kotlinx.datetime.LocalDate

data class PortfolioSnapshot(
    val date: LocalDate,
    val totalValueBase: Double,
    val baseCurrency: String,
)
