package app.claro.tv.data.repository

import app.claro.tv.data.Result
import app.claro.tv.models.ContentItem
import app.claro.tv.models.TvChannel

/**
 * PUBLIC_INTERFACE
 * Repository interface for content data operations.
 * Abstracts data source (API, local cache, fake data) from UI.
 */
interface ContentRepository {
    
    /**
     * PUBLIC_INTERFACE
     * Fetches Continue Watching items for a user.
     *
     * @param userId User identifier
     * @return Result containing list of ContentItem or error
     */
    suspend fun getContinueWatching(userId: String): Result<List<ContentItem>>
    
    /**
     * PUBLIC_INTERFACE
     * Fetches list of available TV channels.
     *
     * @return Result containing list of TvChannel or error
     */
    suspend fun getTvChannels(): Result<List<TvChannel>>
}
