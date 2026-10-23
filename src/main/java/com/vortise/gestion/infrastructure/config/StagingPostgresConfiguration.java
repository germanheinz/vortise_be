package com.vortise.gestion.infrastructure.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import java.util.List;

/** Validates staging datasource inputs before DataSource or JPA beans can initialize. */
@Configuration(proxyBeanMethods = false)
@Profile("staging")
public class StagingPostgresConfiguration {
    @Bean
    static BeanFactoryPostProcessor stagingPostgresConfigurationGuard(Environment environment) {
        return beanFactory -> validateDatasource(
            environment.getProperty("APP_DATABASE_JDBC_URL"),
            environment.getProperty("APP_DATABASE_USERNAME"),
            environment.getProperty("APP_DATABASE_PASSWORD"),
            environment.getProperty("spring.datasource.url"),
            environment.getProperty("spring.datasource.username"),
            environment.getProperty("spring.datasource.password"),
            environment.getProperty("spring.datasource.driver-class-name"),
            environment.getProperty("spring.sql.init.mode"),
            environment.getProperty("spring.jpa.hibernate.ddl-auto"),
            environment.getProperty("spring.flyway.enabled"),
            environment.getProperty("spring.h2.console.enabled"),
            hasAlternativeDatasourcePath(environment)
        );
    }

    static void validateDatasource(String url, String username, String password) {
        validateDatasource(url, username, password, url, username, password, "org.postgresql.Driver",
            "never", "validate", "true", "false", false);
    }

    static void validateDatasource(String url, String username, String password,
            String effectiveUrl, String effectiveUsername, String effectivePassword, String driver,
            String sqlInitMode, String ddlAuto, String flywayEnabled, String h2ConsoleEnabled,
            boolean alternativeDatasourcePath) {
        if (url == null || url.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                "Staging requires non-empty APP_DATABASE_JDBC_URL, APP_DATABASE_USERNAME and APP_DATABASE_PASSWORD"
            );
        }
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("APP_DATABASE_JDBC_URL must use the PostgreSQL JDBC scheme");
        }
        if (!url.equals(effectiveUrl) || !username.equals(effectiveUsername) || !password.equals(effectivePassword)) {
            throw new IllegalStateException("Effective staging datasource must exactly match APP_DATABASE_* settings");
        }
        if (!"org.postgresql.Driver".equals(driver) || !"never".equalsIgnoreCase(sqlInitMode)
                || !"validate".equalsIgnoreCase(ddlAuto) || !"true".equalsIgnoreCase(flywayEnabled)
                || !"false".equalsIgnoreCase(h2ConsoleEnabled) || alternativeDatasourcePath) {
            throw new IllegalStateException("Staging must use PostgreSQL, Flyway-only schema management and a disabled H2 console");
        }
    }

    private static boolean hasAlternativeDatasourcePath(Environment environment) {
        List<String> forbidden = List.of(
            "spring.datasource.hikari.jdbc-url",
            "spring.datasource.hikari.data-source-class-name",
            "spring.datasource.hikari.data-source-properties.url",
            "spring.datasource.jndi-name",
            "spring.datasource.type",
            "spring.jpa.properties.hibernate.connection.url",
            "spring.jpa.properties.jakarta.persistence.jdbc.url",
            "spring.jpa.properties.hibernate.connection.provider_class",
            "hibernate.connection.url",
            "jakarta.persistence.jdbc.url",
            "javax.persistence.jdbc.url"
        );
            return forbidden.stream().anyMatch(environment::containsProperty);
    }
}
