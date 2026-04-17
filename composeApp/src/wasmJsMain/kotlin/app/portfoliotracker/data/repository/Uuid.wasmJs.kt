package app.portfoliotracker.data.repository

internal actual fun generateUuid(): String = generateUuidJs()

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => crypto.randomUUID()")
private external fun generateUuidJs(): String
