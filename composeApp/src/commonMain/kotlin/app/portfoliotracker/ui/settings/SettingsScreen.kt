package app.portfoliotracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val CURRENCIES = listOf("EUR", "USD", "GBP", "HUF", "CHF", "SEK", "NOK", "DKK", "PLN", "CZK")
private val REFRESH_OPTIONS = listOf(1, 6, 12, 24, 48, 168) // hours

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // Base currency
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Base Currency", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }) {
                    Text(state.baseCurrency)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    for (c in CURRENCIES) {
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = {
                                viewModel.setBaseCurrency(c)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        // Refresh interval
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Refresh Interval", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }) {
                    Text(formatInterval(state.refreshIntervalHours))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    for (h in REFRESH_OPTIONS) {
                        DropdownMenuItem(
                            text = { Text(formatInterval(h)) },
                            onClick = {
                                viewModel.setRefreshIntervalHours(h)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        // API key
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Finnhub API Key", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.finnhubApiKey,
                    onValueChange = { viewModel.setFinnhubApiKey(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter API key (optional)") },
                    singleLine = true,
                )
                Text(
                    "Free key from finnhub.io for US equity quotes",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}

private fun formatInterval(hours: Int): String = when {
    hours < 24 -> "${hours}h"
    hours == 24 -> "Daily"
    hours == 168 -> "Weekly"
    else -> "${hours / 24}d"
}
