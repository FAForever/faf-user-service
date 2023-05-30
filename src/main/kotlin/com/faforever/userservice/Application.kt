package com.faforever.userservice

import io.micronaut.runtime.Micronaut.build
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = Info(
        title = "faf-user-service",
        version = "2.0",
    ),
)
object Api

fun main(args: Array<String>) {
    build()
        .args(*args)
        .packages("com.faforever")
        .start()
}
