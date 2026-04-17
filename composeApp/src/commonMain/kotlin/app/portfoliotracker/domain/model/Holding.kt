package app.portfoliotracker.domain.model

data class Holding(
    val instrument: Instrument,
    val totalQuantity: Double,
    val totalCost: Double,
    val currentPrice: Double?,
    val currency: String,
) {
    val currentValue: Double?
        get() = currentPrice?.let { it * totalQuantity }

    val gainLoss: Double?
        get() = currentValue?.let { it - totalCost }

    val gainLossPercent: Double?
        get() = if (totalCost != 0.0) gainLoss?.let { it / totalCost * 100.0 } else null

    companion object {
        fun fromTransactions(
            instrument: Instrument,
            transactions: List<Transaction>,
            currentPrice: Double? = null,
        ): Holding? {
            if (transactions.isEmpty()) return null

            var totalQty = 0.0
            var totalCost = 0.0

            for (tx in transactions.sortedBy { it.date }) {
                when (tx.type) {
                    TransactionType.BUY -> {
                        totalQty += tx.quantity
                        totalCost += tx.totalAmount + tx.fee
                    }
                    TransactionType.SELL -> {
                        if (totalQty > 0) {
                            val fraction = tx.quantity / totalQty
                            totalCost -= totalCost * fraction
                        }
                        totalQty -= tx.quantity
                    }
                    else -> { /* dividends, fees etc. don't affect position */ }
                }
            }

            return Holding(
                instrument = instrument,
                totalQuantity = totalQty,
                totalCost = totalCost,
                currentPrice = currentPrice,
                currency = instrument.currency,
            )
        }
    }
}
