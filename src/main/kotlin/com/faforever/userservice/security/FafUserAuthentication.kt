package com.faforever.userservice.security

import io.micronaut.security.authentication.Authentication

class FafUserAuthentication(
    val id: Int,
    private val name: String,
    val scopes: List<String>,
    val roles: List<String>,
    private val attributes: Map<String, Any>
) : Authentication {
    override fun getName(): String = name
    override fun getAttributes(): Map<String, Any> = attributes
}
