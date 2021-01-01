package com.faforever.usermanagement

import com.faforever.usermanagement.config.FafProperties
import com.faforever.usermanagement.domain.SecurityProperties
import com.faforever.usermanagement.hydra.HydraProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(HydraProperties::class, SecurityProperties::class, FafProperties::class)
class UserManagementApplication

fun main() {
    runApplication<UserManagementApplication>()
}
