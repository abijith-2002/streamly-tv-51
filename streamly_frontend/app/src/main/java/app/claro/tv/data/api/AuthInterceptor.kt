package app.claro.tv.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * PUBLIC_INTERFACE
 * OkHttp interceptor that adds Bearer token authentication to API requests.
 * Token is retrieved from TokenProvider.
 *
 * @param tokenProvider Provider that supplies authentication token
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get token from provider
        val token = tokenProvider.getToken()
        
        // If no token available, proceed without authentication
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }
        
        // Add Bearer token to request
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}

/**
 * PUBLIC_INTERFACE
 * Interface for providing authentication tokens.
 * Implementations can retrieve tokens from secure storage, in-memory cache, etc.
 */
interface TokenProvider {
    /**
     * PUBLIC_INTERFACE
     * Returns the current authentication token, or null if not available.
     */
    fun getToken(): String?
}

/**
 * Stub implementation of TokenProvider that returns empty token.
 * Replace with actual implementation that retrieves tokens from secure storage.
 */
class StubTokenProvider : TokenProvider {
    override fun getToken(): String? {
        // TODO: Implement actual token retrieval from secure storage
        // For now, return null (no authentication)
        return null
    }
}
