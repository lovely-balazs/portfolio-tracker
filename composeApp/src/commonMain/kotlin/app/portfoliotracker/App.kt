package app.portfoliotracker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.portfoliotracker.data.database.PortfolioDatabase
import app.portfoliotracker.data.import.ImportOrchestrator
import app.portfoliotracker.data.parser.IbkrCsvParser
import app.portfoliotracker.data.parser.IbkrFlexXmlParser
import app.portfoliotracker.data.parser.LightyearCsvParser
import app.portfoliotracker.data.pricing.CoinGeckoClient
import app.portfoliotracker.data.pricing.FinnhubClient
import app.portfoliotracker.data.pricing.FxRateService
import app.portfoliotracker.data.pricing.PriceRepository
import app.portfoliotracker.data.pricing.PriceService
import app.portfoliotracker.data.pricing.RefreshOrchestrator
import app.portfoliotracker.data.pricing.YahooFinanceClient
import app.portfoliotracker.data.repository.InstrumentRepository
import app.portfoliotracker.data.repository.SettingsRepository
import app.portfoliotracker.data.repository.TransactionRepository
import app.portfoliotracker.ui.dashboard.DashboardScreen
import app.portfoliotracker.ui.dashboard.DashboardViewModel
import app.portfoliotracker.ui.import.ImportScreen
import app.portfoliotracker.ui.import.ImportViewModel
import app.portfoliotracker.ui.manual.ManualEntryScreen
import app.portfoliotracker.ui.manual.ManualEntryViewModel
import app.portfoliotracker.platform.pickFileAndRead
import app.portfoliotracker.ui.navigation.Screen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import app.portfoliotracker.ui.settings.SettingsScreen
import app.portfoliotracker.ui.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@Composable
fun App(database: PortfolioDatabase) {
    val httpClient = remember {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
            }
        }
    }

    val instrumentRepo = remember { InstrumentRepository(database) }
    val transactionRepo = remember { TransactionRepository(database) }
    val priceRepo = remember { PriceRepository(database) }
    val settingsRepo = remember { SettingsRepository(database) }
    val fxRateService = remember { FxRateService(httpClient) }
    val finnhubClient = remember { FinnhubClient(httpClient, "") }
    val yahooClient = remember { YahooFinanceClient(httpClient) }
    val coinGeckoClient = remember { CoinGeckoClient(httpClient) }
    val priceService = remember { PriceService(finnhubClient, yahooClient, coinGeckoClient, priceRepo) }
    val refreshOrchestrator = remember {
        RefreshOrchestrator(priceService, fxRateService, priceRepo, instrumentRepo, transactionRepo)
    }

    val dashboardVM = remember {
        DashboardViewModel(instrumentRepo, transactionRepo, priceRepo, refreshOrchestrator, fxRateService, settingsRepo)
    }
    val parsers = remember { listOf(LightyearCsvParser(), IbkrFlexXmlParser(), IbkrCsvParser()) }
    val importVM = remember { ImportViewModel(ImportOrchestrator(parsers, instrumentRepo, transactionRepo)) }
    val settingsVM = remember { SettingsViewModel(settingsRepo) }
    val manualEntryVM = remember { ManualEntryViewModel(instrumentRepo, transactionRepo) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    LaunchedEffect(currentScreen) {
        if (currentScreen is Screen.Dashboard) {
            dashboardVM.loadDashboard()
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                is Screen.Dashboard -> DashboardScreen(
                    viewModel = dashboardVM,
                    onNavigateToImport = { currentScreen = Screen.Import },
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onNavigateToManualEntry = { currentScreen = Screen.ManualEntry },
                )

                is Screen.Import -> ImportScreen(
                    viewModel = importVM,
                    onPickFile = {
                        pickFileAndRead { content ->
                            MainScope().launch { importVM.importFile(content) }
                        }
                    },
                    onBack = {
                        importVM.reset()
                        currentScreen = Screen.Dashboard
                    },
                )

                is Screen.Settings -> SettingsScreen(
                    viewModel = settingsVM,
                    onBack = { currentScreen = Screen.Dashboard },
                )

                is Screen.ManualEntry -> ManualEntryScreen(
                    viewModel = manualEntryVM,
                    onBack = {
                        manualEntryVM.reset()
                        currentScreen = Screen.Dashboard
                    },
                )
            }
        }
    }
}
