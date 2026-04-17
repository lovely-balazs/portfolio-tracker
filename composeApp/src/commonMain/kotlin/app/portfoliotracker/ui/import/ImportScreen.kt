package app.portfoliotracker.ui.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onPickFile: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Import Transactions", style = MaterialTheme.typography.headlineMedium)

        when (val s = state) {
            is ImportState.Idle -> {
                Text(
                    "Select a CSV or XML file from your broker export.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onPickFile) {
                    Text("Select File")
                }
            }

            is ImportState.Importing -> {
                CircularProgressIndicator()
                Text("Importing...")
            }

            is ImportState.Done -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Import Complete", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Imported: ${s.result.imported}")
                        Text("Duplicates skipped: ${s.result.duplicates}")
                        if (s.result.skipped > 0) {
                            Text("Rows skipped: ${s.result.skipped}")
                        }
                        if (s.result.warnings.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Warnings:", style = MaterialTheme.typography.titleSmall)
                            for (w in s.result.warnings) {
                                Text("- $w", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.reset() }) {
                    Text("Import Another")
                }
            }

            is ImportState.Error -> {
                Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.reset() }) {
                    Text("Try Again")
                }
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}
