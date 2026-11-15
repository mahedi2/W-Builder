package com.example.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.backup.ExportSummary
import com.example.data.backup.ImportConflictStrategy
import com.example.data.backup.ImportSummary
import com.example.data.backup.WBuilderArchiveManager
import com.example.domain.model.WidgetProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data class InProgress(val message: String) : BackupUiState
    data class ExportSuccess(val summary: ExportSummary) : BackupUiState
    data class ImportSuccess(val summary: ImportSummary) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

class BackupRestoreViewModel(
    private val archiveManager: WBuilderArchiveManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportProject(project: WidgetProject, destinationUri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.InProgress("Packaging \"${project.name}\"...")
            archiveManager.exportProjectToUri(project, destinationUri)
                .onSuccess { summary ->
                    _uiState.value = BackupUiState.ExportSuccess(summary)
                }
                .onFailure { error ->
                    _uiState.value = BackupUiState.Error(
                        error.localizedMessage ?: "Failed to export .wbuilder file."
                    )
                }
        }
    }

    fun exportFullBackup(destinationUri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.InProgress("Creating full studio backup...")
            archiveManager.exportFullStudioBackupToUri(destinationUri)
                .onSuccess { summary ->
                    _uiState.value = BackupUiState.ExportSuccess(summary)
                }
                .onFailure { error ->
                    _uiState.value = BackupUiState.Error(
                        error.localizedMessage ?: "Failed to create studio backup."
                    )
                }
        }
    }

    fun importArchive(
        sourceUri: Uri,
        strategy: ImportConflictStrategy = ImportConflictStrategy.GENERATE_NEW_IDS
    ) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.InProgress("Restoring .wbuilder package...")
            archiveManager.importFromUri(sourceUri, strategy)
                .onSuccess { summary ->
                    _uiState.value = BackupUiState.ImportSuccess(summary)
                }
                .onFailure { error ->
                    _uiState.value = BackupUiState.Error(
                        error.localizedMessage ?: "Could not parse .wbuilder archive."
                    )
                }
        }
    }

    fun clearStatus() {
        _uiState.value = BackupUiState.Idle
    }

    companion object {
        fun provideFactory(archiveManager: WBuilderArchiveManager): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { BackupRestoreViewModel(archiveManager) }
            }
    }
}
