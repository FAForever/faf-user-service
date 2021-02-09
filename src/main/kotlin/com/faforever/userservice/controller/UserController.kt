package com.faforever.userservice.controller

import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.security.Principal

@RestController
@RequestMapping(path = ["/user"])
class UserController {

    @PreAuthorize("hasRole('USER')")
    @GetMapping(path = ["/me"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun me(principal: Mono<Principal>): Mono<String> {
        return principal
            .map { "principal: $it" }
    }
}
