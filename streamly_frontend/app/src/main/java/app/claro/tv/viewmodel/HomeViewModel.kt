package app.claro.tv.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.claro.tv.data.Result
import app.claro.tv.data.repository.ContentRepository
import app.claro.tv.models.ContentItem
import app.claro.tv.models.TvChannel
import kotlinx.coroutines.launch

/**
 * PUBLIC_INTERFACE
 * ViewModel for HomeFragment.
 * Manages data fetching, loading states, and error handling for home screen content.
 *
 * @param repository ContentRepository for data operations
 */
class HomeViewModel(
    private val repository: ContentRepository
) : ViewModel() {
    
    // Continue Watching state
    private val _continueWatchingState = MutableLiveData<UiState<List<ContentItem>>>()
    val continueWatchingState: LiveData<UiState<List<ContentItem>>> = _continueWatchingState
    
    // TV Channels state
    private val _tvChannelsState = MutableLiveData<UiState<List<TvChannel>>>()
    val tvChannelsState: LiveData<UiState<List<TvChannel>>> = _tvChannelsState
    
    /**
     * PUBLIC_INTERFACE
     * Fetches Continue Watching data for a user.
     *
     * @param userId User identifier
     */
    fun loadContinueWatching(userId: String = "default_user") {
        _continueWatchingState.value = UiState.Loading
        
        viewModelScope.launch {
            when (val result = repository.getContinueWatching(userId)) {
                is Result.Success -> {
                    _continueWatchingState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _continueWatchingState.value = UiState.Error(result.message)
                }
                is Result.Loading -> {
                    _continueWatchingState.value = UiState.Loading
                }
            }
        }
    }
    
    /**
     * PUBLIC_INTERFACE
     * Fetches TV Channels data.
     */
    fun loadTvChannels() {
        _tvChannelsState.value = UiState.Loading
        
        viewModelScope.launch {
            when (val result = repository.getTvChannels()) {
                is Result.Success -> {
                    _tvChannelsState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _tvChannelsState.value = UiState.Error(result.message)
                }
                is Result.Loading -> {
                    _tvChannelsState.value = UiState.Loading
                }
            }
        }
    }
    
    /**
     * PUBLIC_INTERFACE
     * Retries loading Continue Watching data.
     */
    fun retryContinueWatching(userId: String = "default_user") {
        loadContinueWatching(userId)
    }
    
    /**
     * PUBLIC_INTERFACE
     * Retries loading TV Channels data.
     */
    fun retryTvChannels() {
        loadTvChannels()
    }
}

/**
 * PUBLIC_INTERFACE
 * Sealed class representing UI state for data loading.
 *
 * @param T Type of data
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
