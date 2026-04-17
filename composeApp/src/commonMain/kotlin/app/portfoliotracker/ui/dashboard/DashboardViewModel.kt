package app.portfoliotracker.ui.dashboard

import app.portfoliotracker.data.pricing.FxRateService
import app.portfoliotracker.data.pricing.FxRates
import app.portfoliotracker.data.pricing.PriceRepository
import app.portfoliotracker.data.pricing.RefreshOrchestrator
import app.portfoliotracker.data.repository.InstrumentRepository
import app.portfoliotracker.data.repository.TransactionRepository
import app.portfoliotracker.domain.HoldingsCalculator
import app.portfoliotracker.domain.PortfolioSummary
import app.portfoliotracker.domain.model.PortfolioSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel(
    private val instrumentRepo: InstrumentRepository,
    private val transactionRepo: TransactionRepository,
    private val priceRepo: PriceRepository,
    private val refreshOrchestrator: RefreshOrchestrator,
    private val fxRateService: FxRateService,
) {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    fun loadDashboard(baseCurrency: String = "EUR") {
        val instruments = instrumentRepo.getAll()
        val txnsByInstrument = instruments.associate { it.id to transactionRepo.getByInstrument(it.id) }
        val latestPrices = priceRepo.getAllLatestPrices().associateBy { it.instrumentId }
        val snapshots = priceRepo.getAllPortfolioSnapshots()

        // Use cached FX rates or empty
        val fxRates = FxRates(base = baseCurrency, rates = emptyMap())

        val summary = HoldingsCalculator.calculate(
            instruments = instruments,
            transactionsByInstrument = txnsByInstrument,
            latestPrices = latestPrices,
            fxRates = fxRates,
            baseCurrency = baseCurrency,
        )

        _state.value = DashboardState(
            summary = summary,
            portfolioSnapshots = snapshots,
            isRefreshing = false,
        )
    }

    suspend fun refreshPrices(baseCurrency: String = "EUR") {
        _state.value = _state.value.copy(isRefreshing = true)
        try {
            refreshOrchestrator.refresh(baseCurrency)
            loadDashboard(baseCurrency)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isRefreshing = false,
                error = e.message,
            )
        }
    }
}

data class DashboardState(
    val summary: PortfolioSummary? = null,
    val portfolioSnapshots: List<PortfolioSnapshot> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
)
