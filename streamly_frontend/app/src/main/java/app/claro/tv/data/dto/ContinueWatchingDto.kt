package app.claro.tv.data.dto

import com.google.gson.annotations.SerializedName

/**
 * PUBLIC_INTERFACE
 * Data Transfer Object for Continue Watching items from API.
 * Maps to ContentItem domain model.
 *
 * @param id Unique content identifier
 * @param title Content title
 * @param subtitle Optional subtitle/description
 * @param artworkUrl URL to thumbnail/poster image
 * @param progress Playback progress (0.0 to 1.0)
 */
data class ContinueWatchingDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("subtitle")
    val subtitle: String? = null,
    
    @SerializedName("artwork_url")
    val artworkUrl: String? = null,
    
    @SerializedName("progress")
    val progress: Float = 0f
)

/**
 * PUBLIC_INTERFACE
 * Response wrapper for Continue Watching API endpoint.
 */
data class ContinueWatchingResponse(
    @SerializedName("items")
    val items: List<ContinueWatchingDto>
)
