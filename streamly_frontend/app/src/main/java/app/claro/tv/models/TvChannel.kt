package app.claro.tv.models

/**
 * PUBLIC_INTERFACE
 * Data model representing a live TV channel with program information.
 * 
 * @param id Unique channel identifier
 * @param programTitle Current program title
 * @param channelNumber Channel number display (e.g., "004")
 * @param channelName Channel name (e.g., "Claro Sports")
 * @param startTime Program start time (e.g., "11:30")
 * @param endTime Program end time (e.g., "12:30")
 * @param isLive Whether the channel is currently live
 * @param isRentable Whether the content requires rental
 * @param thumbnailUrl URL or resource path for the program thumbnail
 * @param progress Playback progress for recorded content (0.0 to 1.0)
 */
data class TvChannel(
    val id: String,
    val programTitle: String,
    val channelNumber: String,
    val channelName: String,
    val startTime: String,
    val endTime: String,
    val isLive: Boolean = true,
    val isRentable: Boolean = false,
    val thumbnailUrl: String = "",
    val progress: Float = 0.0f
)
