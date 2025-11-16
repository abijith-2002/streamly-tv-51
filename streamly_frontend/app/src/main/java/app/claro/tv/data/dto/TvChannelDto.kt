package app.claro.tv.data.dto

import com.google.gson.annotations.SerializedName

/**
 * PUBLIC_INTERFACE
 * Data Transfer Object for TV Channel from API.
 * Maps to TvChannel domain model.
 *
 * @param id Unique channel identifier
 * @param name Channel name
 * @param logoUrl URL to channel logo
 * @param isLive Whether channel is currently live
 * @param currentProgramTitle Current program title
 * @param currentProgramTimeWindow Time window string (e.g., "11:30 - 12:30")
 * @param channelNumber Channel number display
 * @param thumbnailUrl URL to program thumbnail
 * @param progress Playback progress for recorded content
 */
data class TvChannelDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("logo_url")
    val logoUrl: String? = null,
    
    @SerializedName("is_live")
    val isLive: Boolean = false,
    
    @SerializedName("current_program_title")
    val currentProgramTitle: String? = null,
    
    @SerializedName("current_program_time_window")
    val currentProgramTimeWindow: String? = null,
    
    @SerializedName("channel_number")
    val channelNumber: String? = null,
    
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
    
    @SerializedName("progress")
    val progress: Float = 0f,
    
    @SerializedName("is_rentable")
    val isRentable: Boolean = false
)

/**
 * PUBLIC_INTERFACE
 * Response wrapper for TV Channels API endpoint.
 */
data class TvChannelsResponse(
    @SerializedName("channels")
    val channels: List<TvChannelDto>
)
