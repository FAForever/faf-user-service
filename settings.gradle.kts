rootProject.name = "faf-user-service"

pluginManagement {
    repositories {
        mavenLocal()
        maven(url = "https://repo.spring.io/release")
        maven(url = "https://repo.spring.io/milestone")
        maven(url = "https://repo.spring.io/snapshot")
        gradlePluginPortal()
        mavenCentral()
    }
}
