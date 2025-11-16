package app.claro.tv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.claro.tv.data.repository.ContentRepository

/**
 * PUBLIC_INTERFACE
 * Factory for creating HomeViewModel with dependency injection.
 *
 * @param repository ContentRepository instance to inject
 */
class HomeViewModelFactory(
    private val repository: ContentRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
