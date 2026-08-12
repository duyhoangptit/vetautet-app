package com.vetautet.app.infrastructure.config;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DataSourceConfig {

    @Bean
    ApplicationRunner hikariPoolLogger(DataSource dataSource) {
        return args -> {
            if (dataSource instanceof HikariDataSource hikari) {
                log.info("=== HikariCP Pool Configuration ===");
                log.info("Pool name       : {}", hikari.getPoolName());
                log.info("JDBC URL        : {}", hikari.getJdbcUrl());
                log.info("Auto-commit     : {}", hikari.isAutoCommit());
                log.info("Min idle        : {}", hikari.getMinimumIdle());
                log.info("Max pool size   : {}", hikari.getMaximumPoolSize());
                log.info("Conn timeout    : {} ms", hikari.getConnectionTimeout());
                log.info("Idle timeout    : {} ms", hikari.getIdleTimeout());
                log.info("Max lifetime    : {} ms", hikari.getMaxLifetime());
                log.info("===================================");
            }
        };
    }
}
 