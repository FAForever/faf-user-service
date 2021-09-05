package com.faforever.userservice.backend.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class MetricHelper(meterRegistry: MeterRegistry) {
    companion object {
        private const val USER_REGISTRATIONS_COUNT = "user.registrations.count"
        private const val USER_PASSWORD_RESET_COUNT = "user.password.reset.count"
        private const val STEP_TAG = "step"
        private const val MODE_TAG = "step"
    }

    // User Registration Counters
    val userRegistrationCounter: Counter = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG, "registration"
    )
    val userActivationCounter: Counter = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG, "activation"
    )
    val userSteamLinkRequestedCounter: Counter = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG, "steamLinkRequested"
    )
    val userSteamLinkDoneCounter: Counter = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG, "steamLinkDone"
    )
    val userSteamLinkFailedCounter: Counter = meterRegistry.counter(
        USER_REGISTRATIONS_COUNT,
        STEP_TAG, "steamLinkFailed"
    )

    // Username Change Counters
    val userNameChangeCounter: Counter = meterRegistry.counter("user.name.change.count")

    // Password Reset Counters
    val userPasswordResetRequestCounter: Counter = meterRegistry.counter(
        USER_PASSWORD_RESET_COUNT,
        STEP_TAG, "request",
        MODE_TAG, "email"
    )
    val userPasswordResetViaSteamRequestCounter: Counter = meterRegistry.counter(
        USER_PASSWORD_RESET_COUNT,
        STEP_TAG, "request",
        MODE_TAG, "steam"
    )
    val userPasswordResetDoneCounter: Counter = meterRegistry.counter(
        USER_PASSWORD_RESET_COUNT,
        STEP_TAG, "done",
        MODE_TAG, "email"
    )
    val userPasswordResetFailedCounter: Counter = meterRegistry.counter(
        USER_PASSWORD_RESET_COUNT,
        STEP_TAG, "failed",
        MODE_TAG, "email"
    )
    val userPasswordResetDoneViaSteamCounter: Counter = meterRegistry.counter(
        USER_PASSWORD_RESET_COUNT,
        STEP_TAG, "done",
        MODE_TAG, "steam"
    )
    val userPasswordResetFailedViaSteamCounter: Counter = meterRegistry.counter(
        USER_PASSWORD_RESET_COUNT,
        STEP_TAG, "failed",
        MODE_TAG, "steam"
    )
}
