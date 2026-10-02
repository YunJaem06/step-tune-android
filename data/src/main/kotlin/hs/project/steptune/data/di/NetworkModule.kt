package hs.project.steptune.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hs.project.steptune.api.AuthAPI
import hs.project.steptune.api.MusicRecommendationAPI
import hs.project.steptune.api.StepAPI
import hs.project.steptune.api.client.AccessTokenAuthenticator
import hs.project.steptune.api.client.BearerAuthInterceptor
import hs.project.steptune.data.config.NetworkConfig
import hs.project.steptune.util.NetworkLogMasker
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        bearerAuthInterceptor: BearerAuthInterceptor,
        accessTokenAuthenticator: AccessTokenAuthenticator,
        httpLoggingInterceptor: HttpLoggingInterceptor,
        config: NetworkConfig
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(bearerAuthInterceptor)
        .authenticator(accessTokenAuthenticator)
        .apply {
            if (config.enableHttpLogging) {
                addInterceptor(httpLoggingInterceptor)
            }
        }
        .build()

    @Provides
    @Singleton
    @RefreshAuthClient
    fun provideRefreshAuthOkHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor,
        config: NetworkConfig
    ): OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (config.enableHttpLogging) {
                addInterceptor(httpLoggingInterceptor)
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(config: NetworkConfig): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message ->
            config.logMessage(NetworkLogMasker.mask(message))
        }.apply {
            level = if (config.enableHttpLogging) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            redactHeader("X-API-Key")
        }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, config: NetworkConfig): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    @RefreshAuthClient
    fun provideRefreshAuthRetrofit(
        @RefreshAuthClient okHttpClient: OkHttpClient,
        config: NetworkConfig
    ): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAuthAPI(retrofit: Retrofit): AuthAPI =
        retrofit.create(AuthAPI::class.java)

    @Provides
    @Singleton
    fun provideStepAPI(retrofit: Retrofit): StepAPI =
        retrofit.create(StepAPI::class.java)

    @Provides
    @Singleton
    fun provideMusicRecommendationAPI(retrofit: Retrofit): MusicRecommendationAPI =
        retrofit.create(MusicRecommendationAPI::class.java)

    @Provides
    @Singleton
    @RefreshAuthClient
    fun provideRefreshAuthAPI(
        @RefreshAuthClient retrofit: Retrofit
    ): AuthAPI = retrofit.create(AuthAPI::class.java)
}
