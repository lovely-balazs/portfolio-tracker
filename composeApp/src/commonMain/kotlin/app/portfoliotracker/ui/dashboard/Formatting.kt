package app.portfoliotracker.ui.dashboard

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

fun formatAmount(v: Double): String = formatDecimal(v, 2)
fun formatQty(v: Double): String {
    return if (v == v.toLong().toDouble()) v.toLong().toString()
    else formatDecimal(v, 4)
}
fun formatPct(v: Double): String = formatDecimal(v, 1)

private fun formatDecimal(v: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = (abs(v) * factor).roundToLong()
    val intPart = rounded / factor.toLong()
    val fracPart = rounded % factor.toLong()
    val sign = if (v < 0) "-" else ""
    return "$sign$intPart.${fracPart.toString().padStart(decimals, '0')}"
}
