package hs.project.steptune.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hs.project.steptune.data.repository.PedometerRepositoryImpl
import hs.project.steptune.data.repository.SettingsRepositoryImpl
import hs.project.steptune.data.repository.AuthRepositoryImpl
import hs.project.steptune.data.repository.LocalUserDataRepositoryImpl
import hs.project.steptune.domain.repository.AuthRepository
import hs.project.steptune.domain.repository.LocalUserDataRepository
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        repositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLocalUserDataRepository(
        repositoryImpl: LocalUserDataRepositoryImpl
    ): LocalUserDataRepository

    @Binds
    @Singleton
    abstract fun bindPedometerRepository(
        repositoryImpl: PedometerRepositoryImpl
    ): PedometerRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        repositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}

