package app.portfoliotracker.platform

actual class BackgroundRefresh {
    actual fun schedule(intervalHours: Int) {
        // iOS background refresh requires BGTaskScheduler registration in Info.plist
        // and AppDelegate setup. For MVP, this is a no-op stub.
    }

    actual fun cancel() {
        // No-op for iOS MVP
    }
}
