package com.kimght.limbusscreentranslator.core.di

import com.kimght.limbusscreentranslator.data.network.Downloader
import com.kimght.limbusscreentranslator.data.network.OkHttpDownloader
import com.kimght.limbusscreentranslator.data.network.PreferredIpDns
import com.kimght.limbusscreentranslator.data.network.PreferredIpStore
import com.kimght.limbusscreentranslator.data.network.SharedPreferencesPreferredIpStore
import com.kimght.limbusscreentranslator.data.network.SuccessfulIpEventListenerFactory
import com.kimght.limbusscreentranslator.data.network.SuccessfulIpInterceptor
import com.kimght.limbusscreentranslator.data.network.SuccessfulIpRoutes
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideDns(preferredIpStore: PreferredIpStore): Dns =
        PreferredIpDns(Dns.SYSTEM, preferredIpStore)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        dns: Dns,
        preferredIpStore: PreferredIpStore,
    ): OkHttpClient {
        val routes = SuccessfulIpRoutes()
        return OkHttpClient.Builder()
            .dns(dns)
            .eventListenerFactory(SuccessfulIpEventListenerFactory(routes))
            .addInterceptor(SuccessfulIpInterceptor(preferredIpStore, routes))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {

    @Binds
    @Singleton
    abstract fun bindDownloader(impl: OkHttpDownloader): Downloader

    @Binds
    @Singleton
    abstract fun bindPreferredIpStore(
        impl: SharedPreferencesPreferredIpStore,
    ): PreferredIpStore
}
