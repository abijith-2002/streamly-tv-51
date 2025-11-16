package app.claro.tv.data.mappers

import app.claro.tv.data.dto.ContinueWatchingDto
import app.claro.tv.data.dto.TvChannelDto
import app.claro.tv.models.ContentItem
import app.claro.tv.models.TvChannel

/**
 * PUBLIC_INTERFACE
 * Mapper object for converting DTOs to domain models.
 */
object DataMappers {
    
    /**
     * PUBLIC_INTERFACE
     * Converts ContinueWatchingDto to ContentItem domain model.
     */
    fun ContinueWatchingDto.toContentItem(): ContentItem {
        return ContentItem(
            id = this.id,
            title = this.title,
            thumbnailUrl = this.artworkUrl ?: "",
            progress = this.progress.coerceIn(0f, 1f)
        )
    }
    
    /**
     * PUBLIC_INTERFACE
     * Converts TvChannelDto to TvChannel domain model.
     */
    fun TvChannelDto.toTvChannel(): TvChannel {
        val timeWindow = this.currentProgramTimeWindow ?: ""
        val timeParts = timeWindow.split("-").map { it.trim() }
        val startTime = timeParts.getOrNull(0) ?: ""
        val endTime = timeParts.getOrNull(1) ?: ""
        
        return TvChannel(
            id = this.id,
            programTitle = this.currentProgramTitle ?: "Unknown Program",
            channelNumber = this.channelNumber ?: "000",
            channelName = this.name,
            startTime = startTime,
            endTime = endTime,
            isLive = this.isLive,
            isRentable = this.isRentable,
            thumbnailUrl = this.thumbnailUrl ?: "",
            progress = this.progress.coerceIn(0f, 1f)
        )
    }
}
