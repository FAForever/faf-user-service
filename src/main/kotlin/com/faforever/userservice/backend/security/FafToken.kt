package com.faforever.userservice.backend.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.KClass

sealed interface FafToken {
    fun toType(): FafTokenType

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Registration(val username: String, val email: String) : FafToken {
        override fun toType() = FafTokenType.REGISTRATION
    }

    // key stays id so old tokens still work
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PasswordReset(@JsonProperty("id") val userId: Int) : FafToken {
        override fun toType() = FafTokenType.PASSWORD_RESET
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EmailChange(val userId: Int, val newEmail: String) : FafToken {
        override fun toType() = FafTokenType.EMAIL_CHANGE
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LinkToSteam(val userId: Int) : FafToken {
        override fun toType() = FafTokenType.LINK_TO_STEAM
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AccountDeletion(val userId: Int) : FafToken {
        override fun toType() = FafTokenType.ACCOUNT_DELETION
    }
}

enum class FafTokenType {
    REGISTRATION,
    PASSWORD_RESET,
    EMAIL_CHANGE,
    LINK_TO_STEAM,
    ACCOUNT_DELETION,
    ;

    companion object {
        fun fromTokenClass(type: KClass<out FafToken>): FafTokenType = when (type) {
            FafToken.Registration::class -> REGISTRATION
            FafToken.PasswordReset::class -> PASSWORD_RESET
            FafToken.EmailChange::class -> EMAIL_CHANGE
            FafToken.LinkToSteam::class -> LINK_TO_STEAM
            FafToken.AccountDeletion::class -> ACCOUNT_DELETION
            else -> throw IllegalArgumentException("Unknown token type ${type.simpleName}")
        }
    }
}
