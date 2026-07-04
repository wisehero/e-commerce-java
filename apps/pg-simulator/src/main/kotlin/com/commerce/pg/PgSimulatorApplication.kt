package com.commerce.pg

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import java.util.TimeZone

@ConfigurationPropertiesScan
@EnableAsync
@SpringBootApplication(scanBasePackages = ["com.commerce.pg", "com.commerce.config"])
class PgSimulatorApplication {

    @PostConstruct
    fun started() {
        // set timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }
}

fun main(args: Array<String>) {
    runApplication<PgSimulatorApplication>(*args)
}
