package hs.project.steptune.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hs.project.steptune.BuildConfig
import hs.project.steptune.Config
import hs.project.steptune.api.AuthAPI
import hs.project.steptune.api.client.AccessTokenAuthenticator
import hs.project.steptune.api.client.BearerAuthInterceptor
import hs.project.steptune.util.LogUtil
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
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(bearerAuthInterceptor)
        .authenticator(accessTokenAuthenticator)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(httpLoggingInterceptor)
            }
        }
        .build()

    @Provides
    @Singleton
    @RefreshAuthClient
    fun provideRefreshAuthOkHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(httpLoggingInterceptor)
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message ->
            LogUtil.d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(Config.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    @RefreshAuthClient
    fun provideRefreshAuthRetrofit(
        @RefreshAuthClient okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(Config.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAuthAPI(retrofit: Retrofit): AuthAPI =
        retrofit.create(AuthAPI::class.java)

    @Provides
    @Singleton
    @RefreshAuthClient
    fun provideRefreshAuthAPI(
        @RefreshAuthClient retrofit: Retrofit
    ): AuthAPI = retrofit.create(AuthAPI::class.java)
}
