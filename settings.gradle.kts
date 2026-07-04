pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val spotlessVersion: String by settings

    plugins {
        id("org.springframework.boot") version springBootVersion apply false
        id("io.spring.dependency-management") version springDependencyManagementVersion apply false
        id("com.diffplug.spotless") version spotlessVersion apply false
        id("org.jetbrains.kotlin.jvm") version kotlinVersion apply false
        id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion apply false
        id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion apply false
    }
}

rootProject.name = "e-commerce-java"

include(
    "apps:commerce-api",
    "apps:pg-simulator",
    "modules:jpa",
    "modules:redis",
    "modules:kafka",
    "supports:jackson",
    "supports:monitoring",
    "supports:logging",
)
