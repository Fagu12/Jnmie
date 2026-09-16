package com.example.domain.usecase

import com.example.domain.model.AppSettings
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> =
        repository.settingsFlow
}

class UpdateSettingsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(transform: (AppSettings) -> AppSettings) =
        repository.updateSettings(transform)
}

class SetAutoSkipIntroUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) =
        repository.setAutoSkipIntro(enabled)
}

class SetAutoSkipOutroUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) =
        repository.setAutoSkipOutro(enabled)
}

class ResetSettingsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke() =
        repository.resetSettings()
}
