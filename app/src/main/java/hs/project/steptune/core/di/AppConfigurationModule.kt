package hs.project.steptune.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hs.project.steptune.BuildConfig
import hs.project.steptune.Config
import hs.project.steptune.data.config.NetworkConfig
import hs.project.steptune.util.LogUtil
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppConfigurationModule {
    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = Config.BASE_URL,
        enableHttpLogging = BuildConfig.DEBUG,
        logMessage = LogUtil::d
    )
}
