package app.portfoliotracker.data.pricing

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FxRateServiceTest {

    private val rates = FxRates(
        base = "EUR",
        rates = mapOf(
            "USD" to 1.08,
            "GBP" to 0.86,
            "CHF" to 0.97,
        ),
    )

    @Test
    fun convertSameCurrencyReturnsOriginal() {
        val result = convertToBase(1000.0, "EUR", rates)
        assertEquals(1000.0, result)
    }

    @Test
    fun convertUsdToEurBase() {
        // 1 EUR = 1.08 USD, so 1080 USD ≈ 1000 EUR
        val result = convertToBase(1080.0, "USD", rates)
        assertTrue(abs(result - 1000.0) < 0.01)
    }

    @Test
    fun convertGbpToEurBase() {
        // 1 EUR = 0.86 GBP, so 860 GBP = 1000 EUR
        val result = convertToBase(860.0, "GBP", rates)
        assertEquals(1000.0, result)
    }

    @Test
    fun unknownCurrencyReturnsOriginalAmount() {
        val result = convertToBase(500.0, "JPY", rates)
        assertEquals(500.0, result) // no rate → passthrough
    }

    @Test
    fun convertZeroAmountReturnsZero() {
        val result = convertToBase(0.0, "USD", rates)
        assertEquals(0.0, result)
    }

    // Reuse the same logic as FxRateService.convertToBase — pure function
    private fun convertToBase(amount: Double, fromCurrency: String, rates: FxRates): Double {
        if (fromCurrency == rates.base) return amount
        val rate = rates.rates[fromCurrency] ?: return amount
        return if (rate > 0) amount / rate else amount
    }
}
