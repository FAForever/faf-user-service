package com.faforever.userservice

import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.domain.SecurityProperties
import com.faforever.userservice.hydra.HydraProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(HydraProperties::class, SecurityProperties::class, FafProperties::class)
class UserServiceApplication

fun main() {
    runApplication<UserServiceApplication>()
}
