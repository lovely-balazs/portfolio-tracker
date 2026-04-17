package app.portfoliotracker.domain

import app.portfoliotracker.data.pricing.FxRates
import app.portfoliotracker.domain.model.Holding
import app.portfoliotracker.domain.model.Instrument
import app.portfoliotracker.domain.model.PriceSnapshot
import app.portfoliotracker.domain.model.Transaction

data class PortfolioSummary(
    val holdings: List<HoldingWithAllocation>,
    val totalValueBase: Double,
    val baseCurrency: String,
)

data class HoldingWithAllocation(
    val holding: Holding,
    val valueBase: Double,
    val allocationPercent: Double,
)

object HoldingsCalculator {

    fun calculate(
        instruments: List<Instrument>,
        transactionsByInstrument: Map<String, List<Transaction>>,
        latestPrices: Map<String, PriceSnapshot>,
        fxRates: FxRates,
        baseCurrency: String,
    ): PortfolioSummary {
        val holdingsWithBase = mutableListOf<Pair<Holding, Double>>()

        for (inst in instruments) {
            val txns = transactionsByInstrument[inst.id] ?: continue
            val price = latestPrices[inst.id]?.price
            val holding = Holding.fromTransactions(inst, txns, price) ?: continue
            if (holding.totalQuantity <= 0) continue

            val nativeValue = holding.currentValue ?: 0.0
            val valueBase = convertToBase(nativeValue, inst.currency, fxRates, baseCurrency)
            holdingsWithBase.add(holding to valueBase)
        }

        val totalBase = holdingsWithBase.sumOf { it.second }

        val result = holdingsWithBase.map { (holding, valueBase) ->
            HoldingWithAllocation(
                holding = holding,
                valueBase = valueBase,
                allocationPercent = if (totalBase > 0) valueBase / totalBase * 100.0 else 0.0,
            )
        }.sortedByDescending { it.valueBase }

        return PortfolioSummary(
            holdings = result,
            totalValueBase = totalBase,
            baseCurrency = baseCurrency,
        )
    }

    private fun convertToBase(
        amount: Double,
        fromCurrency: String,
        fxRates: FxRates,
        baseCurrency: String,
    ): Double {
        if (fromCurrency == baseCurrency) return amount
        val rate = fxRates.rates[fromCurrency] ?: return amount
        return if (rate > 0) amount / rate else amount
    }
}
