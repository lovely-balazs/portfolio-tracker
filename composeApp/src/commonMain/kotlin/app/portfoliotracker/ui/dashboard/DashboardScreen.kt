package app.portfoliotracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val baseCurrency by produceState("EUR") { value = viewModel.baseCurrency() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Portfolio", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNavigateToImport) { Text("Import") }
                OutlinedButton(onClick = onNavigateToSettings) { Text("Settings") }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Total value
        val summary = state.summary
        if (summary != null) {
            Text(
                "${formatAmount(summary.totalValueBase)} $baseCurrency",
                style = MaterialTheme.typography.displaySmall,
            )
        } else {
            Text("No holdings yet", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(8.dp))

        // Refresh button
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { scope.launch { viewModel.refreshPrices() } },
                enabled = !state.isRefreshing,
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text("Refresh Prices")
            }
            OutlinedButton(onClick = onNavigateToManualEntry) { Text("+ Manual") }
        }

        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))

        // Holdings list
        if (summary != null && summary.holdings.isNotEmpty()) {
            Text("Holdings", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            HoldingsList(summary.holdings, baseCurrency)
        }

        Spacer(Modifier.height(24.dp))

        // Allocation
        if (summary != null) {
            AllocationChart(summary.holdings)
        }

        Spacer(Modifier.height(24.dp))

        // Portfolio value over time
        PortfolioValueChart(state.portfolioSnapshots, baseCurrency)
    }
}
