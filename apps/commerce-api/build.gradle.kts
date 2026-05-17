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

    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
}

tasks.bootJar { enabled = true }
tasks.jar { enabled = false }
