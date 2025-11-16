package app.claro.tv.data

/**
 * PUBLIC_INTERFACE
 * Sealed class representing the result of an operation that can succeed or fail.
 * Provides type-safe error handling for repository operations.
 *
 * @param T Type of data returned on success
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with error information.
     */
    data class Error(
        val exception: Exception,
        val message: String = exception.message ?: "Unknown error"
    ) : Result<Nothing>()
    
    /**
     * Represents a loading state.
     */
    object Loading : Result<Nothing>()
}

/**
 * PUBLIC_INTERFACE
 * Extension function to check if Result is Success.
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * PUBLIC_INTERFACE
 * Extension function to get data from Success result, or null otherwise.
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}
