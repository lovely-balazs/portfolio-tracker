package app.portfoliotracker.ui.manual

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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.portfoliotracker.domain.model.AssetClass
import kotlinx.coroutines.launch

private val MANUAL_ASSET_CLASSES = listOf(
    AssetClass.REAL_ESTATE,
    AssetClass.PRIVATE,
    AssetClass.BOND,
    AssetClass.CRYPTO,
    AssetClass.OTHER,
)

private val CURRENCIES = listOf("EUR", "USD", "GBP", "HUF", "CHF")

@Composable
fun ManualEntryScreen(
    viewModel: ManualEntryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add Manual Holding", style = MaterialTheme.typography.headlineMedium)

        if (state.isSaved) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Holding saved!", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.reset() }) { Text("Add Another") }
                        OutlinedButton(onClick = onBack) { Text("Back to Dashboard") }
                    }
                }
            }
            return
        }

        // Name
        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Name") },
            placeholder = { Text("e.g. Apartment in Budapest") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Asset class picker
        var assetExpanded by remember { mutableStateOf(false) }
        Column {
            Text("Asset Class", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { assetExpanded = true }) {
                Text(state.assetClass.name.replace("_", " "))
            }
            DropdownMenu(expanded = assetExpanded, onDismissRequest = { assetExpanded = false }) {
                for (ac in MANUAL_ASSET_CLASSES) {
                    DropdownMenuItem(
                        text = { Text(ac.name.replace("_", " ")) },
                        onClick = {
                            viewModel.updateAssetClass(ac)
                            assetExpanded = false
                        },
                    )
                }
            }
        }

        // Currency picker
        var currExpanded by remember { mutableStateOf(false) }
        Column {
            Text("Currency", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { currExpanded = true }) {
                Text(state.currency)
            }
            DropdownMenu(expanded = currExpanded, onDismissRequest = { currExpanded = false }) {
                for (c in CURRENCIES) {
                    DropdownMenuItem(
                        text = { Text(c) },
                        onClick = {
                            viewModel.updateCurrency(c)
                            currExpanded = false
                        },
                    )
                }
            }
        }

        // Quantity
        OutlinedTextField(
            value = state.quantityText,
            onValueChange = { viewModel.updateQuantity(it) },
            label = { Text("Quantity") },
            placeholder = { Text("1") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Cost basis
        OutlinedTextField(
            value = state.costBasisText,
            onValueChange = { viewModel.updateCostBasis(it) },
            label = { Text("Total Cost Basis") },
            placeholder = { Text("e.g. 200000") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Current value
        OutlinedTextField(
            value = state.currentValueText,
            onValueChange = { viewModel.updateCurrentValue(it) },
            label = { Text("Current Value (optional)") },
            placeholder = { Text("e.g. 250000") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Notes
        OutlinedTextField(
            value = state.notes,
            onValueChange = { viewModel.updateNotes(it) },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )

        // Error
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { viewModel.save() } }) { Text("Save") }
            OutlinedButton(onClick = onBack) { Text("Cancel") }
        }
    }
}
