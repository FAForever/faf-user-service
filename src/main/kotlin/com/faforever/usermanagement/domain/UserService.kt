package com.faforever.usermanagement.domain

import com.faforever.usermanagement.hydra.HydraService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import sh.ory.hydra.model.AcceptLoginRequest
import sh.ory.hydra.model.GenericError

sealed class LoginResult {
    object UserOrCredentialsMismatch : LoginResult()
    data class SuccessfulLogin(val redirectTo: String) : LoginResult()
    data class UserBanned(val redirectTo: String, val ban: Ban): LoginResult()
}

@Component
class UserService(
    val userRepository: UserRepository,
    val banRepository: BanRepository,
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
                    if (loginRequest.skip || passwordEncoder.matches(password, user.password)) {
                        findGlobalBan(user)
                            .flatMap<LoginResult> { ban ->
                                log.debug("User '$username' is banned by $ban")
                                hydraService.rejectLoginRequest(challenge, GenericError("user_banned"))
                                    .map { LoginResult.UserBanned(it.redirectTo, ban) }
                            }
                            .switchIfEmpty {
                                log.debug("User '$username' logged in successfully")

                                hydraService.acceptLoginRequest(
                                    challenge,
                                    AcceptLoginRequest(user.id.toString())
                                ).map { LoginResult.SuccessfulLogin(it.redirectTo) }
                            }

                    } else {
                        log.debug("Password for user '$username' doesn't match.")
                        Mono.just(LoginResult.UserOrCredentialsMismatch)
                    }
                }
                .switchIfEmpty {
                    log.debug("User '$username' not found")
                    Mono.just(LoginResult.UserOrCredentialsMismatch)
                }
        }

    fun findGlobalBan(user: User): Mono<Ban> = banRepository.findByPlayerIdAndLevel(user.id, BanLevel.GLOBAL)
        .filter { it.isActive }
        .next()
}
