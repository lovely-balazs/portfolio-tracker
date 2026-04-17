package app.portfoliotracker

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.portfoliotracker.data.database.DatabaseDriverFactory
import app.portfoliotracker.data.database.PortfolioDatabase
import app.portfoliotracker.data.database.initSchema
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val driver = DatabaseDriverFactory().createDriver()
    MainScope().launch {
        initSchema(driver)
        val database = PortfolioDatabase(driver)
        ComposeViewport(document.body!!) { App(database) }
    }
}
