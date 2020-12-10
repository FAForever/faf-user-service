package com.faforever.usermanagement.domain

import com.faforever.usermanagement.hydra.HydraService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import sh.ory.hydra.model.AcceptLoginRequest

sealed class LoginResult {
    object FailedLogin : LoginResult()
    data class SuccessfulLogin(val redirectTo: String) : LoginResult()
}

@Component
class UserService(
    val userRepository: UserRepository,
    val hydraService: HydraService,
    val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(UserService::class.java)
    }

    fun login(
        challenge: String,
        username: String,
        password: String,
    ): Mono<LoginResult> = hydraService.getLoginRequest(challenge)
        .flatMap { loginRequest ->
            // TODO: Reject login after x failed attempts
            userRepository.findByUsername(username)
                .flatMap { user ->
                    // TODO: Check for bans
                    if (loginRequest.skip || passwordEncoder.matches(password, user.password)) {
                        log.debug("User '$username' logged in successfully")

                        hydraService.acceptLoginRequest(
                            challenge,
                            AcceptLoginRequest(user.id.toString())
                        ).map { LoginResult.SuccessfulLogin(it.redirectTo) }
                    } else {
                        log.debug("Password for user '$username' doesn't match.")
                        Mono.just(LoginResult.FailedLogin)
                    }
                }
                .switchIfEmpty {
                    log.debug("User '$username' not found")
                    Mono.just(LoginResult.FailedLogin)
                }
        }
}
