package com.faforever.userservice.error

/**
 * A unified response payload for all HTTP services.
 */
class ErrorResponse(val errors: List<Error>) {
    companion object {
        fun ofSingleStringError(errorMessage: String): ErrorResponse {
            return ErrorResponse(listOf(StringOnlyErrorMessage(errorMessage)))
        }
    }
}
