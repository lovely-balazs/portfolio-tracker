package app.portfoliotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.portfoliotracker.data.database.DatabaseDriverFactory
import app.portfoliotracker.data.database.PortfolioDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driver = DatabaseDriverFactory(this).createDriver()
        val database = PortfolioDatabase(driver)
        setContent { App(database) }
    }
}
