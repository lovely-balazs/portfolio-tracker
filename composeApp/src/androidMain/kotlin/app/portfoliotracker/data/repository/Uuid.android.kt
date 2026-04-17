package app.portfoliotracker.data.repository

import java.util.UUID

internal actual fun generateUuid(): String = UUID.randomUUID().toString()
