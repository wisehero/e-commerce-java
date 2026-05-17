plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(project(":modules:jpa"))
    implementation(project(":modules:redis"))
    implementation(project(":modules:kafka"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:monitoring"))
    implementation(project(":supports:logging"))
}

tasks.bootJar { enabled = true }
tasks.jar { enabled = false }
