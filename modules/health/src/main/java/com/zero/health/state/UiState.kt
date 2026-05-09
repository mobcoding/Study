package com.zero.health.state

/**
 * @date:2025/12/24 10:50
 * @path:com.zero.health.state.UiState
 */
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()

    object Loading : UiState<Nothing>()

    data class Success<T>(val data: T) : UiState<T>()

    data class Failed(val throwable: Throwable) : UiState<Nothing>()
}
