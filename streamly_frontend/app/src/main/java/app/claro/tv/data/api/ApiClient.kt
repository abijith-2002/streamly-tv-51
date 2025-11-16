package app.claro.tv.data.api

import app.claro.tv.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * PUBLIC_INTERFACE
 * Factory object for creating Retrofit API clients.
 * Configures OkHttp with logging, authentication, and timeouts.
 */
object ApiClient {
    
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    
    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }
    
    /**
     * PUBLIC_INTERFACE
     * Creates OkHttpClient with configured interceptors and timeouts.
     *
     * @param tokenProvider Provider for authentication tokens
     * @return Configured OkHttpClient instance
     */
    fun createOkHttpClient(tokenProvider: TokenProvider = StubTokenProvider()): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    /**
     * PUBLIC_INTERFACE
     * Creates Retrofit instance with configured base URL and OkHttp client.
     *
     * @param baseUrl API base URL (defaults to BuildConfig value)
     * @param okHttpClient OkHttp client instance
     * @return Configured Retrofit instance
     */
    fun createRetrofit(
        baseUrl: String = BuildConfig.STREAMLY_API_BASE_URL,
        okHttpClient: OkHttpClient = createOkHttpClient()
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * PUBLIC_INTERFACE
     * Creates StreamlyApiService instance.
     *
     * @param retrofit Retrofit instance (defaults to configured instance)
     * @return StreamlyApiService for making API calls
     */
    fun createStreamlyApiService(
        retrofit: Retrofit = createRetrofit()
    ): StreamlyApiService {
        return retrofit.create(StreamlyApiService::class.java)
    }
}
