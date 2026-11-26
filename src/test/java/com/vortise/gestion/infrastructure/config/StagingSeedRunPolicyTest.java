package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class StagingSeedRunPolicyTest {
    @Test
    void stagingNeverUsesEmptyDatabaseBootstrap() {
        assertFalse(new StagingSeedRunPolicy().shouldRunSeeders());
    }

    @Test
    void stagingPolicyAndH2DatabasePolicyAreMutuallyProfileGated() {
        Profile staging = StagingSeedRunPolicy.class.getAnnotation(Profile.class);
        Profile h2 = DatabaseInitializationPolicy.class.getAnnotation(Profile.class);

        assertNotNull(staging);
        assertNotNull(h2);
        assertEquals(java.util.Set.of("staging", "prod", "production", "e2e-local"), java.util.Set.of(staging.value()));
        assertEquals("!staging & !prod & !production & !e2e-local", h2.value()[0]);
    }

    @Test
    void stagingConfigurationRequiresExplicitPostgresSettingsAndDisablesSchemaScripts() throws IOException {
        String configuration;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application-staging.yml")) {
            assertNotNull(input);
            configuration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(configuration.contains("${APP_DATABASE_JDBC_URL}"));
        assertTrue(configuration.contains("${APP_DATABASE_USERNAME}"));
        assertTrue(configuration.contains("${APP_DATABASE_PASSWORD}"));
        assertTrue(configuration.contains("org.postgresql.Driver"));
        assertTrue(configuration.contains("ddl-auto: validate"));
        assertTrue(configuration.contains("mode: never"));
        assertTrue(configuration.contains("enabled: true"));
        assertFalse(configuration.contains("APP_DATABASE_JDBC_URL:"));
    }
}
