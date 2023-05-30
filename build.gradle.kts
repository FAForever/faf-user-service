import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    val kotlinVersion = "1.8.21"

    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.micronaut.application") version "3.5.3"

    // /****** Additional tooling *****/
    // // OpenAPI code generation
    // id("org.openapi.generator") version "4.3.1"
    // Code formatting
    id("com.diffplug.spotless") version "6.19.0"
}

val version: String by project
group = "com.faforever"
java.sourceCompatibility = JavaVersion.VERSION_17

val kotlinVersion = project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    kapt("io.micronaut:micronaut-http-validation")
    kapt("io.micronaut.data:micronaut-data-processor")
    kapt("io.micronaut.openapi:micronaut-openapi")
    kapt("io.micronaut.security:micronaut-security-annotations")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("org.springframework.security:spring-security-crypto:6.0.2")
    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("io.micronaut.data:micronaut-data-r2dbc")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.views:micronaut-views-thymeleaf")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.1.0")
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-prometheus")
    implementation("io.micronaut.tracing:micronaut-tracing-jaeger")
    implementation("io.swagger.core.v3:swagger-annotations")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.7")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.mariadb:r2dbc-mariadb")
    runtimeOnly("io.r2dbc:r2dbc-h2")
    implementation("io.micronaut:micronaut-validation")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("io.projectreactor:reactor-test:3.5.6")
    val mockitoVersion = "4.8.0"
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    val mockserverVersion = "5.14.0"
    testImplementation("org.mock-server:mockserver-netty:$mockserverVersion")
    testImplementation("org.mock-server:mockserver-client-java:$mockserverVersion")
}

application {
    mainClass.set("com.faforever.userservice.ApplicationKt")
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    named<DockerBuildImage>("dockerBuild") {
        images.empty()
        images.add("faforever/faf-user-service")
    }
}

graalvmNative.toolchainDetection.set(false)
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.faforever.*")
    }
}

docker {
    registryCredentials {
        val envUsername = System.getenv("DOCKER_USERNAME")
        val envPassword = System.getenv("DOCKER_PASSWORD")

        if (envUsername != null && envPassword != null) {
            println("Setting up Docker registry login")
            username.set(envUsername)
            password.set(envPassword)
        } else {
            println("No docker credentials defined")
        }
    }
}

spotless {
    val ktlintVersion = "0.46.1"
    kotlin {
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts")

        ktlint(ktlintVersion)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Summarize test results
    addTestListener(object : TestListener {
        val ANSI_BOLD_WHITE = "\u001B[0;1m"
        val ANSI_RESET = "\u001B[0m"
        val ANSI_BLACK = "\u001B[30m"
        val ANSI_RED = "\u001B[31m"
        val ANSI_GREEN = "\u001B[32m"
        val ANSI_YELLOW = "\u001B[33m"
        val ANSI_BLUE = "\u001B[34m"
        val ANSI_PURPLE = "\u001B[35m"
        val ANSI_CYAN = "\u001B[36m"
        val ANSI_WHITE = "\u001B[37m"
        val BALLOT_CHECKED = "\uD83D\uDDF9"
        val BALLOT_UNCHECKED = "\u2610"
        val BALLOT_CROSS = "\uD83D\uDDF7"

        override fun beforeSuite(suite: TestDescriptor) {
            if (suite.name.startsWith("Test Run") || suite.name.startsWith("Gradle Worker")) {
                return
            }

            if (suite.parent != null && suite.className != null) {
                println(ANSI_BOLD_WHITE + suite.name + ANSI_RESET)
            }
        }

        override fun beforeTest(testDescriptor: TestDescriptor?) {
        }

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            val indicator = when {
                result.failedTestCount > 0 -> ANSI_RED + BALLOT_CROSS
                result.skippedTestCount > 0 -> ANSI_YELLOW + BALLOT_UNCHECKED
                else -> ANSI_GREEN + BALLOT_CHECKED
            }

            println("    $indicator$ANSI_RESET ${testDescriptor.name}")
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent != null && suite.className != null) {
                println("")
            }

            if (suite.parent == null) { // will match the outermost suite
                val successStyle = ANSI_GREEN
                val skipStyle = ANSI_YELLOW
                val failStyle = ANSI_RED
                val summaryStyle = when (result.resultType) {
                    TestResult.ResultType.SUCCESS -> successStyle
                    TestResult.ResultType.SKIPPED -> skipStyle
                    TestResult.ResultType.FAILURE -> failStyle
                }

                println(
                    """
                        --------------------------------------------------------------------------
                        Results: $summaryStyle${result.resultType}$ANSI_RESET (${result.testCount} tests, $successStyle${result.successfulTestCount} passed$ANSI_RESET, $failStyle${result.failedTestCount} failed$ANSI_RESET, $skipStyle${result.skippedTestCount} skipped$ANSI_RESET)
                        --------------------------------------------------------------------------
                    """.trimIndent()
                )
            }
        }
    })
}
