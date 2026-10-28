package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

class JwtServiceConfigurationTest {
    @Test
    void rejectsInvalidOrOverOneHourExpiration() {
        String key = "test-only-signing-key-with-enough-length-1234567890";
        assertThrows(IllegalStateException.class, () -> new JwtService(key, 0));
        assertThrows(IllegalStateException.class, () -> new JwtService(key, 3_600_001));
    }
    @Test
    void rejectsMissingSigningSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 60_000));
    }

    @Test
    void rejectsWeakSigningSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short-test-key", 60_000));
    }

    @Test
    void missingPropertyFailsInMinimalSpringContextWithoutDataSourceOrJpa() {
        minimalContext().run(context -> {
            Throwable failure = context.getStartupFailure();
            assertNotNull(failure);
            org.junit.jupiter.api.Assertions.assertTrue(hasCause(failure, IllegalArgumentException.class));
        });
    }

    @Test
    void weakPropertyFailsInMinimalSpringContextWithoutDataSourceOrJpa() {
        minimalContext().withPropertyValues("app.security.jwt-secret=short-test-key").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(hasCause(context.getStartupFailure(), IllegalStateException.class));
        });
    }

    private static ApplicationContextRunner minimalContext() {
        return new ApplicationContextRunner().withUserConfiguration(JwtServiceTestConfiguration.class);
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(JwtService.class)
    static class JwtServiceTestConfiguration {
        @Bean
        static PropertySourcesPlaceholderConfigurer propertyPlaceholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
