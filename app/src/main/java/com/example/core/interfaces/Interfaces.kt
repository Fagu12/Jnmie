package com.example.core.interfaces

/**
 * Backward compatibility alias mappings for Clean Architecture.
 * Domain interfaces are defined in com.example.domain.repository.
 */

typealias AnimeRepository = com.example.domain.repository.AnimeRepository
typealias AniListRepository = com.example.domain.repository.AniListRepository
typealias ProviderManager = com.example.domain.repository.ProviderManager
typealias SourceResolver = com.example.domain.repository.SourceResolver
typealias PlaybackRepository = com.example.domain.repository.PlaybackRepository
typealias SettingsRepository = com.example.domain.repository.SettingsRepository
typealias SearchRepository = com.example.domain.repository.SearchRepository
typealias AppSettings = com.example.domain.model.AppSettings
