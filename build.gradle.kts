val projectGroup: String by project
val projectVersion: String by project
val javaToolchainVersion: String by project
val springBootVersion: String by project

plugins {
    java
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

allprojects {
    group = projectGroup
    version = projectVersion
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(javaToolchainVersion.toInt())
        }
    }

    extensions.configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    dependencies {
        // Lombok — 모든 모듈 공통 (BaseJpaEntity 등에서 @Getter 사용)
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")

        // @ConfigurationProperties 메타데이터 생성 (IDE 자동완성·application.yml 키 검증)
        "annotationProcessor"("org.springframework.boot:spring-boot-configuration-processor")

        // Bean Validation (hibernate-validator) 런타임 제공 — @Valid/@NotNull 등 활성
        "runtimeOnly"("org.springframework.boot:spring-boot-starter-validation")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        // JUnit 5 launcher (IntelliJ가 테스트 발견하는 데 필요)
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // DB·Redis 컨테이너를 공유하는 통합 테스트의 격리를 위해 직렬 실행
        maxParallelForks = 1
        // BaseJpaEntity ZonedDateTime 등 시간 일관성
        systemProperty("user.timezone", "Asia/Seoul")
        // jpa.yml / redis.yml 의 test 프로파일 블록 활성
        systemProperty("spring.profiles.active", "test")
    }
}
