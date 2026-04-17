package app.portfoliotracker

import androidx.compose.ui.window.ComposeUIViewController
import app.portfoliotracker.data.database.DatabaseDriverFactory
import app.portfoliotracker.data.database.PortfolioDatabase

fun MainViewController() = ComposeUIViewController {
    val driver = DatabaseDriverFactory().createDriver()
    val database = PortfolioDatabase(driver)
    App(database)
}
