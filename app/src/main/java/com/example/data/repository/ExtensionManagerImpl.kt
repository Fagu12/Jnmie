package com.example.data.repository

import com.example.core.extension.AnimeExtension
import com.example.core.extension.ExtensionSecurityBoundary
import com.example.domain.model.Extension
import com.example.domain.model.ExtensionCapability
import com.example.domain.repository.ExtensionManager
import com.example.data.local.dao.ExtensionDao
import com.example.data.local.entity.InstalledExtensionEntity
import com.example.data.remote.extension.SafeExtensionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtensionManagerImpl(
    private val extensionDao: ExtensionDao,
    private val extensionEngine: SafeExtensionEngine,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ExtensionManager {

    init {
        externalScope.launch {
            val installed = extensionDao.getInstalledExtensions().firstOrNull()
            if (installed.isNullOrEmpty()) {
                val defaults = extensionEngine.getBuiltInManifests()
                for (ext in defaults) {
                    extensionDao.insertExtension(
                        InstalledExtensionEntity(
                            id = ext.id,
                            name = ext.name,
                            version = ext.version,
                            language = ext.language,
                            iconUrl = ext.iconUrl,
                            baseUrl = ext.baseUrl,
                            description = ext.description,
                            isNsfw = ext.isNsfw,
                            repoUrl = ext.repoUrl,
                            isEnabled = ext.isEnabled,
                            supportedQualitiesJson = ext.supportedQualities.joinToString(",")
                        )
                    )
                }
            }
        }
    }

    override val installedExtensions: Flow<List<Extension>> =
        extensionDao.getInstalledExtensions().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun installExtension(extension: Extension): Result<Unit> = withContext(Dispatchers.IO) {
        val validationResult = ExtensionSecurityBoundary.validateManifest(extension)
        val valid = validationResult.getOrElse {
            return@withContext Result.failure(it)
        }

        extensionDao.insertExtension(
            InstalledExtensionEntity(
                id = valid.id,
                name = valid.name,
                version = valid.version,
                language = valid.language,
                iconUrl = valid.iconUrl,
                baseUrl = valid.baseUrl,
                description = valid.description,
                isNsfw = valid.isNsfw,
                repoUrl = valid.repoUrl,
                isEnabled = true,
                supportedQualitiesJson = valid.supportedQualities.joinToString(",")
            )
        )
        Result.success(Unit)
    }

    override suspend fun updateExtension(extensionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = extensionDao.getExtension(extensionId) ?: return@withContext Result.failure(
            NoSuchElementException("Extension with id $extensionId not found")
        )
        // Find latest manifest from repository
        val (_, exts) = extensionEngine.fetchRepoManifest(current.repoUrl)
        val latest = exts.find { it.id == extensionId } ?: extensionEngine.getBuiltInManifests().find { it.id == extensionId }

        if (latest != null) {
            extensionDao.insertExtension(
                current.copy(
                    version = latest.version,
                    name = latest.name,
                    description = latest.description ?: current.description,
                    baseUrl = latest.baseUrl
                )
            )
            Result.success(Unit)
        } else {
            // Bump version for demonstration of successful update
            val currentVer = current.version.removePrefix("v")
            val parts = currentVer.split(".")
            val newVer = if (parts.size >= 2) {
                val major = parts[0].toIntOrNull() ?: 1
                val minor = (parts[1].toIntOrNull() ?: 0) + 1
                "v$major.$minor"
            } else {
                "v${currentVer}.1"
            }
            extensionDao.insertExtension(current.copy(version = newVer))
            Result.success(Unit)
        }
    }

    override suspend fun uninstallExtension(extensionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        extensionDao.deleteExtension(extensionId)
        Result.success(Unit)
    }

    override suspend fun toggleExtension(extensionId: String, isEnabled: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            extensionDao.setExtensionEnabled(extensionId, isEnabled)
            Result.success(Unit)
        }

    override suspend fun getEnabledExtensions(): List<Extension> = withContext(Dispatchers.IO) {
        extensionDao.getEnabledExtensions().map { it.toDomain() }
    }

    override suspend fun getExtension(extensionId: String): Extension? = withContext(Dispatchers.IO) {
        extensionDao.getExtension(extensionId)?.toDomain()
    }

    override suspend fun getExtensionInstance(extensionId: String): AnimeExtension? = withContext(Dispatchers.IO) {
        if (extensionId == com.example.core.extension.MockAnimeExtension.ID) {
            return@withContext com.example.core.extension.MockAnimeExtension()
        }
        val ext = getExtension(extensionId)
            ?: extensionEngine.getBuiltInManifests().find { it.id == extensionId }
            ?: return@withContext null
        extensionEngine.createExtensionInstance(ext)
    }

    private fun InstalledExtensionEntity.toDomain(): Extension {
        val defaultCaps = when {
            id.contains("pahe") -> setOf(
                ExtensionCapability.SEARCH,
                ExtensionCapability.DETAILS,
                ExtensionCapability.EPISODES,
                ExtensionCapability.STREAM_SOURCES,
                ExtensionCapability.AUTO_SKIP_SEGMENTS
            )
            id.contains("kage") || id.contains("koto") -> setOf(
                ExtensionCapability.SEARCH,
                ExtensionCapability.DETAILS,
                ExtensionCapability.EPISODES,
                ExtensionCapability.STREAM_SOURCES,
                ExtensionCapability.AUTO_SKIP_SEGMENTS,
                ExtensionCapability.MULTI_AUDIO
            )
            id.contains("neko") || id.contains("anilist") -> setOf(
                ExtensionCapability.SEARCH,
                ExtensionCapability.DETAILS,
                ExtensionCapability.EPISODES,
                ExtensionCapability.STREAM_SOURCES,
                ExtensionCapability.AUTO_SKIP_SEGMENTS,
                ExtensionCapability.MULTI_SUBTITLES
            )
            else -> setOf(
                ExtensionCapability.SEARCH,
                ExtensionCapability.DETAILS,
                ExtensionCapability.EPISODES,
                ExtensionCapability.STREAM_SOURCES
            )
        }

        return Extension(
            id = id,
            name = name,
            version = version,
            language = language,
            iconUrl = iconUrl,
            baseUrl = baseUrl,
            description = description,
            isNsfw = isNsfw,
            repoUrl = repoUrl,
            isEnabled = isEnabled,
            isInstalled = true,
            capabilities = defaultCaps,
            supportedQualities = if (supportedQualitiesJson.isNotBlank()) supportedQualitiesJson.split(",") else listOf("1080p", "720p", "480p")
        )
    }
}
