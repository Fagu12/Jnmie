package com.example.ui.screens.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.extension.ExtensionSecurityBoundary
import com.example.domain.model.Extension
import com.example.domain.model.Repository
import com.example.domain.repository.ExtensionManager
import com.example.domain.repository.ExtensionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExtensionsUiState(
    val selectedSection: String = "Extensions", // "Extensions" or "Repositories"
    val query: String = "",
    val filterTag: String = "All", // "All", "Installed", "Available", "Updates", "Fast", "Dual Audio"
    val installedExtensions: List<Extension> = emptyList(),
    val availableExtensions: List<Extension> = emptyList(),
    val repositories: List<Repository> = emptyList(),
    val selectedExtension: Extension? = null,
    val isRefreshing: Boolean = false,
    val isAddingRepo: Boolean = false,
    val refreshingRepoUrl: String? = null,
    val updatingExtensionId: String? = null,
    val repoToDelete: Repository? = null,
    val showAddRepoDialog: Boolean = false,
    val urlValidationError: String? = null,
    val statusMessage: String? = null,
    val error: String? = null
) {
    val totalUpdatesCount: Int
        get() = installedExtensions.count { it.hasUpdate }
}

class ExtensionsViewModel(
    private val extensionManager: ExtensionManager,
    private val extensionRepository: ExtensionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtensionsUiState())
    val uiState: StateFlow<ExtensionsUiState> = _uiState.asStateFlow()

    init {
        observeData()
        loadAvailable()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                extensionManager.installedExtensions,
                extensionRepository.getRepositories()
            ) { installed, repos ->
                Pair(installed, repos)
            }.collect { (installed, repos) ->
                _uiState.update { state ->
                    // Annotate installed extensions with update flags based on available extensions
                    val annotatedInstalled = installed.map { inst ->
                        val available = state.availableExtensions.find { it.id == inst.id }
                        val hasUpdate = available != null && isNewerVersion(available.version, inst.version)
                        inst.copy(
                            hasUpdate = hasUpdate,
                            latestVersion = available?.version ?: inst.version
                        )
                    }
                    state.copy(
                        installedExtensions = annotatedInstalled,
                        repositories = repos
                    )
                }
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val cleanLatest = latest.removePrefix("v").trim()
        val cleanCurrent = current.removePrefix("v").trim()
        if (cleanLatest == cleanCurrent) return false
        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun loadAvailable() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val res = extensionRepository.getAvailableExtensions(null)
            if (res.isSuccess) {
                val availableList = res.getOrDefault(emptyList())
                _uiState.update { state ->
                    val annotatedInstalled = state.installedExtensions.map { inst ->
                        val available = availableList.find { it.id == inst.id }
                        val hasUpdate = available != null && isNewerVersion(available.version, inst.version)
                        inst.copy(
                            hasUpdate = hasUpdate,
                            latestVersion = available?.version ?: inst.version
                        )
                    }
                    state.copy(
                        isRefreshing = false,
                        availableExtensions = availableList,
                        installedExtensions = annotatedInstalled
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = res.exceptionOrNull()?.message ?: "Failed to fetch extension catalog"
                    )
                }
            }
        }
    }

    fun validateUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return "Repository URL cannot be empty"
        }
        val res = ExtensionSecurityBoundary.validateAndNormalizeRepoUrl(trimmed)
        return if (res.isFailure) {
            res.exceptionOrNull()?.message ?: "Invalid repository URL format"
        } else {
            null
        }
    }

    fun toggleExtension(extensionId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            extensionManager.toggleExtension(extensionId, isEnabled)
        }
    }

    fun uninstallExtension(extensionId: String) {
        viewModelScope.launch {
            extensionManager.uninstallExtension(extensionId)
            _uiState.update { state ->
                val updatedSelected = if (state.selectedExtension?.id == extensionId) null else state.selectedExtension
                state.copy(
                    selectedExtension = updatedSelected,
                    statusMessage = "Extension uninstalled"
                )
            }
        }
    }

    fun installExtension(extension: Extension) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val res = extensionManager.installExtension(extension)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    statusMessage = if (res.isSuccess) "Installed ${extension.name}" else null,
                    error = res.exceptionOrNull()?.message
                )
            }
            loadAvailable()
        }
    }

    fun updateExtension(extensionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingExtensionId = extensionId) }
            val res = extensionManager.updateExtension(extensionId)
            _uiState.update {
                it.copy(
                    updatingExtensionId = null,
                    statusMessage = if (res.isSuccess) "Extension updated successfully" else null,
                    error = res.exceptionOrNull()?.message
                )
            }
            loadAvailable()
        }
    }

    fun updateAllExtensions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val installed = _uiState.value.installedExtensions
            for (ext in installed) {
                if (ext.hasUpdate) {
                    extensionManager.updateExtension(ext.id)
                }
            }
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    statusMessage = "All extensions updated to latest version"
                )
            }
            loadAvailable()
        }
    }

    fun addRepository(url: String) {
        val validationErr = validateUrl(url)
        if (validationErr != null) {
            _uiState.update { it.copy(urlValidationError = validationErr) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingRepo = true, urlValidationError = null, error = null) }
            val res = extensionRepository.addRepository(url.trim())
            if (res.isSuccess) {
                loadAvailable()
                _uiState.update {
                    it.copy(
                        isAddingRepo = false,
                        showAddRepoDialog = false,
                        statusMessage = "Repository added: ${res.getOrNull()?.name}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAddingRepo = false,
                        urlValidationError = res.exceptionOrNull()?.message ?: "Failed to validate repository manifest"
                    )
                }
            }
        }
    }

    fun setRepoToDelete(repo: Repository?) {
        _uiState.update { it.copy(repoToDelete = repo) }
    }

    fun confirmRemoveRepository() {
        val repo = _uiState.value.repoToDelete ?: return
        viewModelScope.launch {
            extensionRepository.removeRepository(repo.url)
            loadAvailable()
            _uiState.update {
                it.copy(
                    repoToDelete = null,
                    statusMessage = "Repository removed"
                )
            }
        }
    }

    fun refreshRepository(repoUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshingRepoUrl = repoUrl, error = null) }
            val res = extensionRepository.refreshRepositories()
            loadAvailable()
            _uiState.update {
                it.copy(
                    refreshingRepoUrl = null,
                    statusMessage = if (res.isSuccess) "Repository synced" else null,
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }

    fun refreshAllRepositories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val res = extensionRepository.refreshRepositories()
            loadAvailable()
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    statusMessage = if (res.isSuccess) "Repositories synchronized" else null,
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }

    fun selectExtension(extension: Extension?) {
        _uiState.update { it.copy(selectedExtension = extension) }
    }

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    fun setSelectedSection(section: String) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    fun setFilterTag(tag: String) {
        _uiState.update { it.copy(filterTag = tag) }
    }

    fun setAddRepoDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddRepoDialog = visible, urlValidationError = null, error = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, error = null) }
    }

    fun retryLoading() {
        refreshAllRepositories()
    }

    class Factory(
        private val extensionManager: ExtensionManager,
        private val extensionRepository: ExtensionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExtensionsViewModel(extensionManager, extensionRepository) as T
        }
    }
}
