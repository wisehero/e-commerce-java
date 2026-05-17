package com.commerce.config.jpa;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan({"com.commerce"})
@EnableJpaRepositories({"com.commerce.infrastructure"})
public class JpaConfig {
}
