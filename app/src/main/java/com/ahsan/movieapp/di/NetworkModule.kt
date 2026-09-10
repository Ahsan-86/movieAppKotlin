package com.ahsan.movieapp.di

import com.ahsan.movieapp.BuildConfig
import com.ahsan.movieapp.data.remote.TmdbApi
import com.ahsan.movieapp.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Appends TMDB's required `api_key` query param to every request, so callers never touch it. */
    private val apiKeyInterceptor = Interceptor { chain ->
        val original = chain.request()
        val urlWithKey = original.url.newBuilder()
            .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
            .build()
        chain.proceed(original.newBuilder().url(urlWithKey).build())
    }

    /**
     * Transparently retries a request up to [maxRetries] times (with a short linear backoff)
     * when the call fails with an [IOException] — a dropped packet, a DNS blip, a connection
     * reset on flaky mobile data. Every TMDB call this app makes is a GET, so retrying is always
     * safe (nothing here has a side effect that could double-fire). Runs on the OkHttp dispatcher
     * thread, not the main thread, so the short sleep between attempts never touches the UI.
     * This is what turns "one bad packet = an error screen" into "one bad packet = invisible."
     */
    private class RetryInterceptor(private val maxRetries: Int = 2) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            var lastException: IOException? = null
            repeat(maxRetries + 1) { attempt ->
                try {
                    return chain.proceed(request)
                } catch (e: IOException) {
                    lastException = e
                    if (attempt < maxRetries) Thread.sleep(300L * (attempt + 1))
                }
            }
            throw lastException ?: IOException("Request failed after $maxRetries retries")
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        // The Explore screen fires 6 category requests at once on first launch; OkHttp's default
        // dispatcher only runs 5 requests per host concurrently, so the 6th used to just queue
        // behind the others — one more small contributor to "first load feels slow." Raised here
        // so all of them go out together.
        val dispatcher = Dispatcher().apply { maxRequestsPerHost = 10 }
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(RetryInterceptor())
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.TMDB_BASE_URL.toHttpUrl())
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideTmdbApi(retrofit: Retrofit): TmdbApi = retrofit.create(TmdbApi::class.java)
}
