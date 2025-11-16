package app.claro.tv.data.repository

import app.claro.tv.data.Result
import app.claro.tv.models.ContentItem
import app.claro.tv.models.TvChannel
import kotlinx.coroutines.delay

/**
 * PUBLIC_INTERFACE
 * Fake in-memory implementation of ContentRepository for testing.
 * Returns hardcoded data without making network calls.
 */
class FakeContentRepository : ContentRepository {
    
    private val fakeContinueWatching = listOf(
        ContentItem("1", "Rogue One", "", 0.404f),
        ContentItem("2", "Ex Machina", "", 0.404f),
        ContentItem("3", "Sing Street", "", 0.404f),
        ContentItem("4", "2012", "", 0.404f),
        ContentItem("5", "Ad Astra", "", 0.404f)
    )
    
    private val fakeTvChannels = listOf(
        TvChannel(
            "1", "Marca Claro Radio", "004", "Claro sports",
            "11:30", "12:30", true, false, "", 0.386f
        ),
        TvChannel(
            "2", "E.T.", "005", "HBO Channel",
            "11:30", "12:30", true, true, "", 0.386f
        ),
        TvChannel(
            "3", "Marca Claro Radio", "004", "Claro sports",
            "11:30", "12:30", true, false, "", 0.386f
        )
    )
    
    override suspend fun getContinueWatching(userId: String): Result<List<ContentItem>> {
        // Simulate network delay
        delay(500)
        return Result.Success(fakeContinueWatching)
    }
    
    override suspend fun getTvChannels(): Result<List<TvChannel>> {
        // Simulate network delay
        delay(500)
        return Result.Success(fakeTvChannels)
    }
}
