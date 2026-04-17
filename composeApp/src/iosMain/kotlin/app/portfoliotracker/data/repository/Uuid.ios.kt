package app.portfoliotracker.data.repository

import platform.Foundation.NSUUID

internal actual fun generateUuid(): String = NSUUID().UUIDString()
