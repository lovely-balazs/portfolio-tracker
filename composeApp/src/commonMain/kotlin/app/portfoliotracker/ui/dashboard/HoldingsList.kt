package app.portfoliotracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.portfoliotracker.domain.HoldingWithAllocation

@Composable
fun HoldingsList(holdings: List<HoldingWithAllocation>, baseCurrency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (h in holdings) {
            HoldingRow(h, baseCurrency)
        }
    }
}

@Composable
private fun HoldingRow(item: HoldingWithAllocation, baseCurrency: String) {
    val h = item.holding
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(h.instrument.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${h.instrument.ticker} · ${formatQty(h.totalQuantity)} shares",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val value = h.currentValue
                if (value != null) {
                    Text(
                        "${formatAmount(value)} ${h.currency}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "≈ ${formatAmount(item.valueBase)} $baseCurrency",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text("N/A", style = MaterialTheme.typography.titleSmall)
                }
                val gl = h.gainLoss
                val glPct = h.gainLossPercent
                if (gl != null && glPct != null) {
                    val prefix = if (gl >= 0) "+" else ""
                    val color = if (gl >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Text(
                        "$prefix${formatAmount(gl)} (${formatPct(glPct)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                    )
                }
                Text(
                    "${formatPct(item.allocationPercent)}%",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

// formatAmount, formatQty, formatPct imported from Formatting.kt
