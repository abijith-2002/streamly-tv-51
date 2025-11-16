package app.claro.tv.models

/**
 * PUBLIC_INTERFACE
 * Data model representing a content item (movie, series, etc.) in the Continue Watching rail.
 * 
 * @param id Unique identifier for the content
 * @param title Display title of the content
 * @param thumbnailUrl URL or resource path for the thumbnail image
 * @param progress Playback progress as a percentage (0.0 to 1.0)
 */
data class ContentItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String = "",
    val progress: Float = 0.0f
)
