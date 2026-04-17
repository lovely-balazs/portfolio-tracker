package app.portfoliotracker.platform

actual class BackgroundRefresh {
    private var timerId: Int? = null

    actual fun schedule(intervalHours: Int) {
        cancel()
        val intervalMs = intervalHours * 3600 * 1000
        timerId = setInterval(intervalMs)
    }

    actual fun cancel() {
        timerId?.let { clearInterval(it) }
        timerId = null
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(ms) => setInterval(() => {}, ms)")
private external fun setInterval(ms: Int): Int

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(id) => clearInterval(id)")
private external fun clearInterval(id: Int)
