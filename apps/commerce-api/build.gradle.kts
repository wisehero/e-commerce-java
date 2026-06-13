plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.security:spring-security-crypto")

    testImplementation("org.springframework.boot:spring-boot-webmvc-test")

    implementation(project(":modules:jpa"))
    implementation(project(":modules:redis"))
    implementation(project(":modules:kafka"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:monitoring"))
    implementation(project(":supports:logging"))

    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    // 통합·동시성 테스트용 실 MySQL (Testcontainers 2.x 좌표, 버전은 spring-boot-dependencies BOM 관리)
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
}

tasks.bootJar { enabled = true }
tasks.jar { enabled = false }
