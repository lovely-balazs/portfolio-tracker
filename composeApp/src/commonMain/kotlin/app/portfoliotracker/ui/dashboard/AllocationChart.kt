package app.portfoliotracker.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.portfoliotracker.domain.HoldingWithAllocation

@Composable
fun AllocationChart(holdings: List<HoldingWithAllocation>) {
    if (holdings.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No holdings", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    // Simple text-based allocation display
    // TODO: Replace with Koala Plot pie chart when chart rendering is stable on wasmJs
    Box(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column {
            Text("Allocation", style = MaterialTheme.typography.titleSmall)
            for (h in holdings) {
                val pct = formatPct(h.allocationPercent)
                Text(
                    "${h.holding.instrument.ticker}: $pct%",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
