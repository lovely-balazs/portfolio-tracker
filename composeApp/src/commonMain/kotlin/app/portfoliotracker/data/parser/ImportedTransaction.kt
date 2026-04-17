package app.portfoliotracker.data.parser

import kotlinx.datetime.LocalDate

data class ImportedTransaction(
    val brokerSource: String,
    val date: LocalDate,
    val type: String,
    val ticker: String?,
    val isin: String?,
    val name: String?,
    val quantity: Double?,
    val pricePerUnit: Double?,
    val totalAmount: Double?,
    val currency: String,
    val fee: Double,
    val fxRate: Double?,
    val notes: String?,
)
