package com.commerce.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 통합 테스트 베이스. 실 MySQL 컨테이너를 JVM당 한 번 띄워(싱글톤) 공유하고,
 * 커스텀 datasource(datasource.mysql-jpa.main.*)에 컨테이너 접속 정보를 주입한다.
 *
 * <p>표준 spring.datasource.* 가 아니라 수동 Hikari 빈(DataSourceConfig)을 쓰므로
 * {@code @ServiceConnection} 대신 @DynamicPropertySource로 배선한다.
 * 컨테이너 props가 모든 하위 테스트에서 동일하므로 스프링 컨텍스트 캐시가 재사용된다.
 */
@SpringBootTest
public abstract class IntegrationTestSupport {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
        .withDatabaseName("commerce");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("datasource.mysql-jpa.main.jdbc-url", MYSQL::getJdbcUrl);
        registry.add("datasource.mysql-jpa.main.username", MYSQL::getUsername);
        registry.add("datasource.mysql-jpa.main.password", MYSQL::getPassword);
        registry.add("datasource.mysql-jpa.main.driver-class-name", MYSQL::getDriverClassName);
        // 동시성 테스트가 커넥션 부족으로 흔들리지 않도록 풀을 넉넉히
        registry.add("datasource.mysql-jpa.main.maximum-pool-size", () -> "50");
        registry.add("datasource.mysql-jpa.main.minimum-idle", () -> "10");
    }
}
