package com.faforever.userservice.backend.security

import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PasswordEncoderTest {
    private val passwordEncoder = PasswordEncoder()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "banana",
            "äääööüüü",
            "⠽∍␣Ⅿ₎⪡⛳ₙ⏦⌒ⱌ⑦⾕",
        ],
    )
    fun `check password match`(password: String) {
        val hashed = passwordEncoder.encode(password)

        assertThat("Hashed password can't be matched with raw password", passwordEncoder.matches(password, hashed))
    }
}
