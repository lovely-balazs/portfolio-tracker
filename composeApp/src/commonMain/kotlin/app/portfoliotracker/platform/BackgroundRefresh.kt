package app.portfoliotracker.platform

expect class BackgroundRefresh {
    fun schedule(intervalHours: Int)
    fun cancel()
}
