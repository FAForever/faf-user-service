package com.faforever.userservice.backend.account

object UsernameValidator {
    private val invalidCharacters = Regex("[^A-Za-z0-9_-]")

    fun startsWithLetter(username: String): Boolean {
        return username.isNotEmpty() && (username[0] in 'A'..'Z' || username[0] in 'a'..'z')
    }

    fun hasValidLength(username: String): Boolean {
        return username.length in 3..16
    }

    fun containsOnlyAllowedCharacters(username: String): Boolean {
        return username.isNotBlank() && !invalidCharacters.containsMatchIn(username)
    }
}
