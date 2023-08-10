package com.faforever.userservice.backend.metrics

import io.micrometer.core.instrument.MeterRegistry
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class MetricHelper(private val meterRegistry: MeterRegistry) {
    companion object {
        private const val USER_REGISTRATIONS_COUNT = "user.registrations.count"
        private const val PASSWORD_RESET_COUNT = "user.password.reset.count"
        private const val STEP_TAG = "step"
        private const val MODE_TAG = "mode"
    }

    // User Registration Counters
    fun incrementUserRegistrationCounter() {
        meterRegistry.counter(
            USER_REGISTRATIONS_COUNT,
            STEP_TAG,
            "registration",
        ).increment()
    }

    fun incrementUserActivationCounter() = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG,
        "activation",
    ).increment()

    fun incrementUserSteamLinkRequestedCounter() = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG,
        "steamLinkRequested",
    ).increment()

    fun incrementUserSteamLinkDoneCounter() = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG,
        "steamLinkDone",
    ).increment()

    fun incrementUserSteamLinkFailedCounter() = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG,
        "steamLinkFailed",
    )

    // Username Change Counters
    fun incrementUserNameChangeCounter() = meterRegistry.counter("user.name.change.count").increment()

    val userPasswordChangeCounter: Counter = meterRegistry.counter("user.password.change.count")

    // Password Reset Counters
    fun incrementPasswordResetViaEmailRequestCounter() = meterRegistry.counter(
        PASSWORD_RESET_COUNT,
        STEP_TAG,
        "request",
        MODE_TAG,
        "email",
    ).increment()

    fun incrementPasswordResetViaEmailSentCounter() = meterRegistry.counter(
        PASSWORD_RESET_COUNT,
        STEP_TAG,
        "emailSent",
        MODE_TAG,
        "email",
    ).increment()

    fun incrementPasswordResetViaEmailDoneCounter() = meterRegistry.counter(
        PASSWORD_RESET_COUNT,
        STEP_TAG,
        "done",
        MODE_TAG,
        "email",
    ).increment()

    fun incrementPasswordResetViaSteamRequestCounter() = meterRegistry.counter(
        PASSWORD_RESET_COUNT,
        STEP_TAG,
        "request",
        MODE_TAG,
        "steam",
    ).increment()

    fun incrementPasswordResetViaSteamDoneCounter() = meterRegistry.counter(
        PASSWORD_RESET_COUNT,
        STEP_TAG,
        "done",
        MODE_TAG,
        "steam",
    ).increment()

    fun incrementPasswordResetViaEmailFailedCounter() = meterRegistry.counter(
        PASSWORD_RESET_COUNT,
        STEP_TAG,
        "failed",
        MODE_TAG,
        "email",
    ).increment()

    fun incrementPasswordResetViaSteamFailedCounter() = meterRegistry.counter(
        PASSWORD_RESET_COUNT,
        STEP_TAG,
        "failed",
        MODE_TAG,
        "steam",
    ).increment()
}
