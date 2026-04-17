package app.portfoliotracker.ui.manual

import app.portfoliotracker.data.repository.InstrumentRepository
import app.portfoliotracker.data.repository.TransactionRepository
import app.portfoliotracker.data.repository.generateUuid
import app.portfoliotracker.domain.model.AssetClass
import app.portfoliotracker.domain.model.Instrument
import app.portfoliotracker.domain.model.Transaction
import app.portfoliotracker.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

class ManualEntryViewModel(
    private val instrumentRepo: InstrumentRepository,
    private val transactionRepo: TransactionRepository,
) {
    private val _state = MutableStateFlow(ManualEntryState())
    val state: StateFlow<ManualEntryState> = _state

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun updateAssetClass(assetClass: AssetClass) {
        _state.value = _state.value.copy(assetClass = assetClass)
    }

    fun updateCurrency(currency: String) {
        _state.value = _state.value.copy(currency = currency)
    }

    fun updateQuantity(qty: String) {
        _state.value = _state.value.copy(quantityText = qty)
    }

    fun updateCostBasis(cost: String) {
        _state.value = _state.value.copy(costBasisText = cost)
    }

    fun updateCurrentValue(value: String) {
        _state.value = _state.value.copy(currentValueText = value)
    }

    fun updateNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun save(): Boolean {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Name is required")
            return false
        }
        val qty = s.quantityText.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _state.value = s.copy(error = "Quantity must be a positive number")
            return false
        }
        val costBasis = s.costBasisText.toDoubleOrNull()
        if (costBasis == null || costBasis < 0) {
            _state.value = s.copy(error = "Cost basis must be a non-negative number")
            return false
        }

        val ticker = "MANUAL-${s.name.uppercase().replace(" ", "-").take(20)}"
        val instrument = instrumentRepo.resolveOrCreate(
            isin = null,
            ticker = ticker,
            name = s.name,
            currency = s.currency,
            assetClass = s.assetClass,
        )

        val pricePerUnit = if (qty > 0) costBasis / qty else 0.0

        @OptIn(kotlin.time.ExperimentalTime::class)
        val todayStr = kotlin.time.Clock.System.now().toString().take(10)
        val today = LocalDate.parse(todayStr)

        val txn = Transaction(
            id = generateUuid(),
            instrumentId = instrument.id,
            brokerSource = "manual",
            type = TransactionType.BUY,
            date = today,
            quantity = qty,
            pricePerUnit = pricePerUnit,
            totalAmount = costBasis,
            currency = s.currency,
            fee = 0.0,
            fxRate = null,
            importHash = null,
            notes = s.notes.ifBlank { null },
        )
        transactionRepo.insert(txn)

        // If current value provided, store as a manual price snapshot
        val currentVal = s.currentValueText.toDoubleOrNull()
        if (currentVal != null && currentVal > 0 && qty > 0) {
            _state.value = s.copy(savedCurrentPrice = currentVal / qty, savedInstrumentId = instrument.id)
        }

        _state.value = ManualEntryState(isSaved = true)
        return true
    }

    fun reset() {
        _state.value = ManualEntryState()
    }
}

data class ManualEntryState(
    val name: String = "",
    val assetClass: AssetClass = AssetClass.REAL_ESTATE,
    val currency: String = "EUR",
    val quantityText: String = "1",
    val costBasisText: String = "",
    val currentValueText: String = "",
    val notes: String = "",
    val error: String? = null,
    val isSaved: Boolean = false,
    val savedCurrentPrice: Double? = null,
    val savedInstrumentId: String? = null,
)
