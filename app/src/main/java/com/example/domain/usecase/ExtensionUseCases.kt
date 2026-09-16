package com.example.domain.usecase

import com.example.domain.model.Extension
import com.example.domain.model.Repository
import com.example.domain.repository.ExtensionManager
import com.example.domain.repository.ExtensionRepository
import kotlinx.coroutines.flow.Flow

class GetRepositoriesUseCase(private val repository: ExtensionRepository) {
    operator fun invoke(): Flow<List<Repository>> =
        repository.getRepositories()
}

class AddRepositoryUseCase(private val repository: ExtensionRepository) {
    suspend operator fun invoke(url: String): Result<Repository> =
        repository.addRepository(url)
}

class RemoveRepositoryUseCase(private val repository: ExtensionRepository) {
    suspend operator fun invoke(url: String): Result<Unit> =
        repository.removeRepository(url)
}

class RefreshRepositoriesUseCase(private val repository: ExtensionRepository) {
    suspend operator fun invoke(): Result<List<Repository>> =
        repository.refreshRepositories()
}

class GetAvailableExtensionsUseCase(private val repository: ExtensionRepository) {
    suspend operator fun invoke(repoUrl: String? = null): Result<List<Extension>> =
        repository.getAvailableExtensions(repoUrl)
}

class GetInstalledExtensionsUseCase(private val manager: ExtensionManager) {
    operator fun invoke(): Flow<List<Extension>> =
        manager.installedExtensions
}

class InstallExtensionUseCase(private val manager: ExtensionManager) {
    suspend operator fun invoke(extension: Extension): Result<Unit> =
        manager.installExtension(extension)
}

class UpdateExtensionUseCase(private val manager: ExtensionManager) {
    suspend operator fun invoke(extensionId: String): Result<Unit> =
        manager.updateExtension(extensionId)
}

class UninstallExtensionUseCase(private val manager: ExtensionManager) {
    suspend operator fun invoke(extensionId: String): Result<Unit> =
        manager.uninstallExtension(extensionId)
}

class ToggleExtensionUseCase(private val manager: ExtensionManager) {
    suspend operator fun invoke(extensionId: String, isEnabled: Boolean): Result<Unit> =
        manager.toggleExtension(extensionId, isEnabled)
}
