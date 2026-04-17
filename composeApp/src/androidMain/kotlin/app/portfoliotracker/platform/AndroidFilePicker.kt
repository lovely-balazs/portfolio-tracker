package app.portfoliotracker.platform

actual fun pickFileAndRead(onResult: (String) -> Unit) {
    // Android file picking requires Activity result APIs — stub for MVP.
    // The import flow works via the wasmJs web target.
}
