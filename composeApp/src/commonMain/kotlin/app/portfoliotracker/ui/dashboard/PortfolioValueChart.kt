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
import app.portfoliotracker.domain.model.PortfolioSnapshot

@Composable
fun PortfolioValueChart(snapshots: List<PortfolioSnapshot>, baseCurrency: String) {
    if (snapshots.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No data yet. Refresh prices to start tracking.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    // Simple text-based value history
    // TODO: Replace with Koala Plot line chart when chart rendering is stable on wasmJs
    Box(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column {
            Text("Portfolio Value Over Time", style = MaterialTheme.typography.titleSmall)
            for (s in snapshots.takeLast(10)) {
                Text(
                    "${s.date}: ${formatAmount(s.totalValueBase)} $baseCurrency",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
