package com.faforever.usermanagement

import com.faforever.usermanagement.hydra.HydraProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(HydraProperties::class)
class UserManagementApplication

fun main() {
    runApplication<UserManagementApplication>()
}
