plugins {
    val kotlinVersion = "1.8.21"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    kotlin("plugin.noarg") version kotlinVersion
    id("com.diffplug.spotless") version "6.19.0"
    id("io.quarkus") version "3.1.2.Final"
    id("com.vaadin") version "24.1.4"
}

defaultTasks("build")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val version: String by project
val quarkusPlatformVersion: String by project
val vaadinVersion: String by project

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.quarkus:quarkus-container-image-docker")
    implementation(enforcedPlatform("com.vaadin:vaadin-bom:$vaadinVersion"))
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusPlatformVersion"))

    implementation("io.quarkus:quarkus-rest-client-reactive-jackson")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-jdbc-mariadb")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-container-image-jib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation("com.vaadin:vaadin-core")
    implementation("com.vaadin:vaadin-core-jandex")
    implementation("com.vaadin:vaadin-quarkus")

    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.quarkus:quarkus-test-security")
    val mockitoVersion = "5.3.1"
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    kotlinOptions.javaParameters = true
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
                    """.trimIndent(),
                )
            }
        }
    })
}
