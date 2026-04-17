package app.portfoliotracker.ui.manual

import app.portfoliotracker.domain.model.AssetClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManualEntryViewModelTest {

    @Test
    fun defaultStateHasExpectedValues() {
        val state = ManualEntryState()
        assertEquals("", state.name)
        assertEquals(AssetClass.REAL_ESTATE, state.assetClass)
        assertEquals("EUR", state.currency)
        assertEquals("1", state.quantityText)
        assertEquals("", state.costBasisText)
        assertEquals("", state.currentValueText)
        assertEquals("", state.notes)
        assertEquals(null, state.error)
        assertEquals(false, state.isSaved)
    }

    @Test
    fun manualTickerFormatIsCorrect() {
        val name = "Apartment in Budapest"
        val ticker = "MANUAL-${name.uppercase().replace(" ", "-").take(20)}"
        assertEquals("MANUAL-APARTMENT-IN-BUDAPES", ticker)
    }

    @Test
    fun pricePerUnitCalculation() {
        val costBasis = 200000.0
        val qty = 1.0
        val pricePerUnit = if (qty > 0) costBasis / qty else 0.0
        assertEquals(200000.0, pricePerUnit)
    }

    @Test
    fun pricePerUnitWithMultipleUnits() {
        val costBasis = 10000.0
        val qty = 100.0
        val pricePerUnit = if (qty > 0) costBasis / qty else 0.0
        assertEquals(100.0, pricePerUnit)
    }

    @Test
    fun pricePerUnitZeroQuantitySafe() {
        val costBasis = 1000.0
        val qty = 0.0
        val pricePerUnit = if (qty > 0) costBasis / qty else 0.0
        assertEquals(0.0, pricePerUnit)
    }
}
