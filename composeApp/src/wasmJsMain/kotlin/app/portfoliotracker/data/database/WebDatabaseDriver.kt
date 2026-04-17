package app.portfoliotracker.data.database

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return createDefaultWebWorkerDriver()
    }
}

suspend fun initSchema(driver: SqlDriver) {
    PortfolioDatabase.Schema.awaitCreate(driver)
}
