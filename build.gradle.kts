plugins {
    val kotlinVersion = "1.6.10"

    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "7.1.1"
    id("io.micronaut.application") version "3.3.2"

    // /****** Additional tooling *****/
    // // OpenAPI code generation
    // id("org.openapi.generator") version "4.3.1"
    // Code formatting
    id("com.diffplug.spotless") version "6.5.1"
}

group = "com.faforever"
version = "snapshot"
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
    implementation("org.springframework.security:spring-security-crypto:5.6.2")
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
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.5")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.mariadb:r2dbc-mariadb")
    runtimeOnly("io.r2dbc:r2dbc-h2")
    implementation("io.micronaut:micronaut-validation")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("io.projectreactor:reactor-test:3.4.16")
    var mockitoVersion = "4.0.0"
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
    testImplementation("org.mock-server:mockserver-netty:5.13.1")
    testImplementation("org.mock-server:mockserver-client-java:5.13.1")
}

application {
    mainClass.set("com.faforever.ApplicationKt")
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

spotless {
    val ktlintVersion = "0.45.2"
    kotlin {
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts")

        ktlint(ktlintVersion)
    }
}
