package app.portfoliotracker.ui.import

import app.portfoliotracker.data.import.ImportOrchestrator
import app.portfoliotracker.data.import.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ImportViewModel(
    private val orchestrator: ImportOrchestrator,
) {
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    fun importFile(content: String) {
        _state.value = ImportState.Importing
        try {
            val result = orchestrator.import(content)
            _state.value = ImportState.Done(result)
        } catch (e: Exception) {
            _state.value = ImportState.Error(e.message ?: "Unknown error")
        }
    }

    fun reset() {
        _state.value = ImportState.Idle
    }
}

sealed class ImportState {
    data object Idle : ImportState()
    data object Importing : ImportState()
    data class Done(val result: ImportResult) : ImportState()
    data class Error(val message: String) : ImportState()
}
