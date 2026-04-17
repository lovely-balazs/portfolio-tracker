package app.portfoliotracker

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.portfoliotracker.data.database.DatabaseDriverFactory
import app.portfoliotracker.data.database.PortfolioDatabase
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val driver = DatabaseDriverFactory().createDriver()
    val database = PortfolioDatabase(driver)
    ComposeViewport(document.body!!) { App(database) }
}
