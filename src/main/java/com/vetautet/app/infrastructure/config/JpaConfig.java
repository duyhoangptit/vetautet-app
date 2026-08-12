package com.vetautet.app.infrastructure.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
* JPA Configuration for Clean Architecture
* Infrastructure layer configuration
*/
@Configuration
@EnableJpaRepositories(basePackages = "com.vetautet.app.infrastructure.persistence.jpa.repository")
@EntityScan(basePackages = "com.vetautet.app.infrastructure.persistence.jpa.entity")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableTransactionManagement
public class JpaConfig {
}
 