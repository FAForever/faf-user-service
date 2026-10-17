package com.faforever.userservice.ui.view.oauth2

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class DeviceLoginViewTest {

    @Test
    fun testNormalizeUserCodeKeepsCanonicalCode() {
        assertThat(DeviceLoginView.normalizeUserCode("3pfmqGJq"), `is`("3pfmqGJq"))
    }

    @Test
    fun testNormalizeUserCodeStripsSeparatorsButKeepsCase() {
        assertThat(DeviceLoginView.normalizeUserCode(" 3pfm-qGJq "), `is`("3pfmqGJq"))
    }

    @Test
    fun testNormalizeUserCodeOfBlankInput() {
        assertThat(DeviceLoginView.normalizeUserCode(null), `is`(""))
        assertThat(DeviceLoginView.normalizeUserCode(""), `is`(""))
        assertThat(DeviceLoginView.normalizeUserCode(" - "), `is`(""))
    }
}
