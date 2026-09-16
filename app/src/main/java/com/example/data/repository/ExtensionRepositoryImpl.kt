package com.example.data.repository

import com.example.core.extension.ExtensionSecurityBoundary
import com.example.domain.model.Extension
import com.example.domain.model.Repository
import com.example.domain.repository.ExtensionRepository
import com.example.data.local.dao.ExtensionDao
import com.example.data.local.entity.ExtensionRepoEntity
import com.example.data.remote.extension.SafeExtensionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtensionRepositoryImpl(
    private val extensionDao: ExtensionDao,
    private val extensionEngine: SafeExtensionEngine,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ExtensionRepository {

    companion object {
        const val DEFAULT_REPO_URL = "https://raw.githubusercontent.com/justanime/anime-extensions/main/index.json"
    }

    init {
        externalScope.launch {
            val existing = extensionDao.getRepositories().firstOrNull()
            if (existing.isNullOrEmpty()) {
                extensionDao.insertRepository(
                    ExtensionRepoEntity(
                        url = DEFAULT_REPO_URL,
                        name = "Just Anime Community Repository",
                        description = "Verified community streaming extensions for anime",
                        extensionCount = extensionEngine.getBuiltInManifests().size,
                        lastRefreshed = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override fun getRepositories(): Flow<List<Repository>> {
        return extensionDao.getRepositories().map { list ->
            list.map {
                Repository(
                    url = it.url,
                    name = it.name,
                    description = it.description,
                    extensionCount = it.extensionCount,
                    lastRefreshed = it.lastRefreshed,
                    branch = "main",
                    author = "Community"
                )
            }
        }
    }

    override suspend fun addRepository(url: String): Result<Repository> = withContext(Dispatchers.IO) {
        val validationResult = ExtensionSecurityBoundary.validateAndNormalizeRepoUrl(url)
        val normalizedUrl = validationResult.getOrElse {
            return@withContext Result.failure(it)
        }

        try {
            val (repo, exts) = extensionEngine.fetchRepoManifest(normalizedUrl)
            val entity = ExtensionRepoEntity(
                url = normalizedUrl,
                name = repo.name,
                description = repo.description ?: "Repository with ${exts.size} extensions",
                extensionCount = exts.size,
                lastRefreshed = System.currentTimeMillis()
            )
            extensionDao.insertRepository(entity)
            Result.success(
                Repository(
                    url = entity.url,
                    name = entity.name,
                    description = entity.description,
                    extensionCount = entity.extensionCount,
                    lastRefreshed = entity.lastRefreshed,
                    branch = repo.branch,
                    author = repo.author
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeRepository(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        extensionDao.deleteRepository(url)
        Result.success(Unit)
    }

    override suspend fun refreshRepositories(): Result<List<Repository>> = withContext(Dispatchers.IO) {
        try {
            val current = extensionDao.getRepositories().firstOrNull() ?: emptyList()
            for (repo in current) {
                val (fetchedRepo, exts) = extensionEngine.fetchRepoManifest(repo.url)
                extensionDao.insertRepository(
                    repo.copy(
                        name = fetchedRepo.name,
                        description = fetchedRepo.description ?: repo.description,
                        extensionCount = exts.size,
                        lastRefreshed = System.currentTimeMillis()
                    )
                )
            }
            val updated = extensionDao.getRepositories().firstOrNull() ?: emptyList()
            Result.success(
                updated.map {
                    Repository(
                        url = it.url,
                        name = it.name,
                        description = it.description,
                        extensionCount = it.extensionCount,
                        lastRefreshed = it.lastRefreshed
                    )
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableExtensions(repoUrl: String?): Result<List<Extension>> =
        withContext(Dispatchers.IO) {
            try {
                if (repoUrl != null) {
                    val (_, exts) = extensionEngine.fetchRepoManifest(repoUrl)
                    Result.success(exts)
                } else {
                    // Fetch across all configured repos
                    val repos = extensionDao.getRepositories().firstOrNull() ?: emptyList()
                    val allExtensions = mutableListOf<Extension>()
                    val seenIds = mutableSetOf<String>()

                    if (repos.isEmpty()) {
                        val (_, defaultExts) = extensionEngine.fetchRepoManifest(DEFAULT_REPO_URL)
                        return@withContext Result.success(defaultExts)
                    }

                    for (repo in repos) {
                        val (_, exts) = extensionEngine.fetchRepoManifest(repo.url)
                        for (ext in exts) {
                            if (seenIds.add(ext.id)) {
                                allExtensions.add(ext)
                            }
                        }
                    }
                    Result.success(allExtensions)
                }
            } catch (e: Exception) {
                Result.success(extensionEngine.getBuiltInManifests())
            }
        }
}
