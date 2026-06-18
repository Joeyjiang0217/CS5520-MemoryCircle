package com.cs5520group15.memorycircle.data

/**
 * Sealed wrapper for async operation outcomes. Repositories return this so
 * ViewModels can branch on Loading / Success / Error without try/catch noise.
 *
 * Usage:
 *   when (result) {
 *       is Result.Loading -> show spinner
 *       is Result.Success -> show data
 *       is Result.Error   -> show error message
 *   }
 */
sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
