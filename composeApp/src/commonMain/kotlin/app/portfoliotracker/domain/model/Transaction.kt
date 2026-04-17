package app.portfoliotracker.domain.model

import kotlinx.datetime.LocalDate

data class Transaction(
    val id: String,
    val instrumentId: String,
    val brokerSource: String,
    val type: TransactionType,
    val date: LocalDate,
    val quantity: Double,
    val pricePerUnit: Double,
    val totalAmount: Double,
    val currency: String,
    val fee: Double,
    val fxRate: Double?,
    val importHash: String?,
    val notes: String?,
)
