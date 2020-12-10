package com.faforever.userservice.error

/**
 * An common interface for Errors to be sent in an ErrorResponse
 */
interface Error {
    val errorCode: String?
    val errorText: String?
}

/**
 * A basic error implementation containing only an error code as a String.
 */
class StringOnlyErrorMessage(
    override val errorText: String,
    override val errorCode: String? = null
) : Error
