package app.claro.tv.data.api

import app.claro.tv.data.dto.ContinueWatchingResponse
import app.claro.tv.data.dto.TvChannelsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * PUBLIC_INTERFACE
 * Retrofit API service interface for Streamly backend.
 * Defines endpoints for fetching content data.
 */
interface StreamlyApiService {
    
    /**
     * PUBLIC_INTERFACE
     * Fetches Continue Watching items for a specific user.
     *
     * @param userId User identifier
     * @return Response containing list of continue watching items
     */
    @GET("v1/users/{userId}/continue-watching")
    suspend fun getContinueWatching(
        @Path("userId") userId: String
    ): Response<ContinueWatchingResponse>
    
    /**
     * PUBLIC_INTERFACE
     * Fetches list of available TV channels with current program info.
     *
     * @return Response containing list of TV channels
     */
    @GET("v1/channels")
    suspend fun getTvChannels(): Response<TvChannelsResponse>
}
