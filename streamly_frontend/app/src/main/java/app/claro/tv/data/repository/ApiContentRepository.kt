package app.claro.tv.data.repository

import app.claro.tv.data.Result
import app.claro.tv.data.api.StreamlyApiService
import app.claro.tv.data.mappers.DataMappers.toContentItem
import app.claro.tv.data.mappers.DataMappers.toTvChannel
import app.claro.tv.models.ContentItem
import app.claro.tv.models.TvChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PUBLIC_INTERFACE
 * API-based implementation of ContentRepository.
 * Fetches data from remote API using Retrofit.
 *
 * @param apiService Retrofit service for API calls
 */
class ApiContentRepository(
    private val apiService: StreamlyApiService
) : ContentRepository {
    
    override suspend fun getContinueWatching(userId: String): Result<List<ContentItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getContinueWatching(userId)
                
                if (response.isSuccessful && response.body() != null) {
                    val items = response.body()!!.items.map { it.toContentItem() }
                    Result.Success(items)
                } else {
                    Result.Error(
                        Exception("API error: ${response.code()} ${response.message()}"),
                        "Failed to fetch continue watching: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Result.Error(
                    e,
                    "Network error: ${e.message}"
                )
            }
        }
    }
    
    override suspend fun getTvChannels(): Result<List<TvChannel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTvChannels()
                
                if (response.isSuccessful && response.body() != null) {
                    val channels = response.body()!!.channels.map { it.toTvChannel() }
                    Result.Success(channels)
                } else {
                    Result.Error(
                        Exception("API error: ${response.code()} ${response.message()}"),
                        "Failed to fetch TV channels: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Result.Error(
                    e,
                    "Network error: ${e.message}"
                )
            }
        }
    }
}
