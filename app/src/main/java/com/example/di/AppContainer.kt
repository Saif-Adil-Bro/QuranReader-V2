package com.example.di

import android.content.Context
import com.example.data.api.QuranApi
import com.example.data.api.QfTokenManager
import com.example.data.repository.QuranRepository
import com.example.data.repository.SettingsRepository
import com.example.util.NetworkUtils
import com.example.util.NoInternetException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Cache
import com.example.BuildConfig
import java.io.File

/**
 * A manual Dependency Injection container.
 * In a more complex app, we could use Hilt or Koin.
 * Using manual DI here for simplicity and fewer build configurations.
 */
class AppContainer(private val context: Context) {
    private val BASE_URL = "https://api.alquran.cloud/v1/"
    private val QURAN_COM_BASE_URL = "https://api.quran.com/api/v4/"

    private val qfTokenManager = QfTokenManager()

    private val okHttpClient: OkHttpClient by lazy {
        val cacheSize = (50 * 1024 * 1024).toLong() // 50 MB
        val cache = Cache(File(context.cacheDir, "http_cache"), cacheSize)

        OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    throw NoInternetException()
                }
                
                val originalRequest = chain.request()
                val urlString = originalRequest.url.toString()
                
                // Check if this request is to the Quran Foundation Content API
                if (urlString.contains("apis.quran.foundation")) {
                val token = qfTokenManager.getAccessToken() ?: ""
                val clientId = BuildConfig.QURAN_FOUNDATION_CLIENT_ID
                
                var request = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .header("x-auth-token", token)
                    .header("x-client-id", clientId)
                    .build()
                
                var response = chain.proceed(request)
                
                // On a 401, clear token, request a fresh one, and retry once.
                if (response.code == 401) {
                    response.close() // Close the old response body before retrying
                    qfTokenManager.clearToken()
                    val newToken = qfTokenManager.getAccessToken() ?: ""
                    
                    request = originalRequest.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "application/json")
                        .header("x-auth-token", newToken)
                        .header("x-client-id", clientId)
                        .build()
                    response = chain.proceed(request)
                }
                response
            } else {
                // Default logic for alquran.cloud and other endpoints
                val request = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
        }.build()
    }

    private val retrofit: Retrofit by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val quranComRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(QURAN_COM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val quranApi: QuranApi by lazy {
        retrofit.create(QuranApi::class.java)
    }

    private val quranComApi: com.example.data.api.QuranComApi by lazy {
        quranComRetrofit.create(com.example.data.api.QuranComApi::class.java)
    }

    val offlineQuranDatabase: com.example.data.local.offline.OfflineQuranDatabase by lazy {
        com.example.data.local.offline.OfflineQuranDatabase.getDatabase(context)
    }

    val quranRepository: QuranRepository by lazy {
        QuranRepository(quranApi, quranComApi, settingsRepository, offlineQuranDatabase.offlineQuranDao(), context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val quranDatabase: com.example.data.local.QuranDatabase by lazy {
        com.example.data.local.QuranDatabase.getDatabase(context)
    }

    val bookmarkDao by lazy {
        quranDatabase.bookmarkDao()
    }

    val memorizedPageDao by lazy {
        quranDatabase.memorizedPageDao()
    }

    val audioRepository: com.example.data.repository.AudioRepository by lazy {
        com.example.data.repository.AudioRepository(context)
    }

    val aiRepository: com.example.data.repository.AiRepository by lazy {
        com.example.data.repository.AiRepository()
    }

    val storageManager: com.example.util.StorageManager by lazy {
        com.example.util.StorageManager(context)
    }

    val mushafDownloader: com.example.data.local.MushafDownloader by lazy {
        com.example.data.local.MushafDownloader(context, storageManager)
    }

    val mushafRepository: com.example.data.repository.MushafRepository by lazy {
        com.example.data.repository.MushafRepository(mushafDownloader, storageManager)
    }

    val postsRepository: com.example.data.repository.PostsRepository by lazy {
        com.example.data.repository.PostsRepository(context)
    }
}
