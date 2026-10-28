package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class StagingSeedRunPolicyTest {
    @Test
    void stagingNeverEnablesDatabaseEmptyDemoSeeders() {
        assertFalse(new StagingSeedRunPolicy().shouldRunSeeders());
    }

    @Test
    void stagingPolicyAndH2DatabasePolicyAreMutuallyProfileGated() {
        Profile staging = StagingSeedRunPolicy.class.getAnnotation(Profile.class);
        Profile h2 = DatabaseInitializationPolicy.class.getAnnotation(Profile.class);
        Profile demoRunner = DataSeederRunner.class.getAnnotation(Profile.class);
        Profile demoUserSeeder = UsuarioDemoSeeder.class.getAnnotation(Profile.class);

        assertNotNull(staging);
        assertNotNull(h2);
        assertNotNull(demoRunner);
        assertNotNull(demoUserSeeder);
        assertEquals("staging", staging.value()[0]);
        assertEquals("!staging & !prod & !production", h2.value()[0]);
        assertEquals("!staging & !prod & !production", demoRunner.value()[0]);
        assertEquals("!staging & !prod & !production", demoUserSeeder.value()[0]);
    }

    @Test
    void stagingSeedPolicyDoesNotInvokeDemoDataSeeder() {
        DataSeeder dataSeeder = mock(DataSeeder.class);

        new DataSeederRunner(dataSeeder, new StagingSeedRunPolicy()).run();

        verifyNoInteractions(dataSeeder);
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
