package app.portfoliotracker.ui.navigation

sealed class Screen {
    data object Dashboard : Screen()
    data object Import : Screen()
    data object Settings : Screen()
    data object ManualEntry : Screen()
}
