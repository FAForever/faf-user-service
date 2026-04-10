import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.allopen)
    alias(libs.plugins.noarg)
    alias(libs.plugins.quarkus)
    alias(libs.plugins.vaadin)
    alias(libs.plugins.spotless)
    alias(libs.plugins.test.logger)
}

// Workaround: Quarkus plugin calls tasks.contains() which triggers realizePending() on all lazy tasks.
// VaadinBuildFrontendTask's constructor calls tasks.withType(Jar, Action) which is forbidden in that
// mutation-guarded context. Force eager realization here (at script eval time, no guard active) so
// the task is already realized by the time Quarkus triggers it.
// See: https://github.com/vaadin/flow/issues/17447
tasks.getByName("vaadinBuildFrontend")

defaultTasks("build")

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

val version: String by project

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(platform(libs.vaadin.bom))
    implementation(enforcedPlatform(libs.quarkus.bom))

    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-container-image-docker")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-mailer")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-jdbc-mariadb")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-rest")
    implementation("com.vaadin:vaadin-core")
    implementation("com.vaadin:vaadin-core-jandex")
    implementation("com.vaadin:vaadin-quarkus")
    implementation("com.vaadin:vaadin-dev")
    implementation("com.nimbusds:nimbus-jose-jwt")

    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation(libs.mockito.kotlin)
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        javaParameters.set(true)
    }
}

/**
 * Open Kotlin (data) classes and provide no-args constructor for Java compatibility
 */
allOpen {
    // Quarkus
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")

    // Hibernate
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

/**
 * Provide no-args constructor for Java compatibility
 */
noArg {
    // Hibernate
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

spotless {
    val ktlintVersion = "0.49.1"
    kotlin {
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts")

        ktlint(ktlintVersion)
    }
}
