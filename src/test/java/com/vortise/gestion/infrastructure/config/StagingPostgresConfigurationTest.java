package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class StagingPostgresConfigurationTest {
    @Test
    void configurationGuardIsLimitedToStaging() {
        Profile profile = StagingPostgresConfiguration.class.getAnnotation(Profile.class);

        org.junit.jupiter.api.Assertions.assertNotNull(profile);
        assertEquals("staging", profile.value()[0]);
    }

    @Test
    void acceptsOnlyExplicitNonEmptyPostgresDatasourceSettings() {
        assertDoesNotThrow(() -> StagingPostgresConfiguration.validateDatasource(
            "jdbc:postgresql://db.example.invalid:5432/gestion", "staging-user", "staging-password"
        ));

        assertThrows(IllegalStateException.class, () -> StagingPostgresConfiguration.validateDatasource(
            "jdbc:h2:mem:unexpected", "staging-user", "staging-password"
        ));
        assertThrows(IllegalStateException.class, () -> StagingPostgresConfiguration.validateDatasource(
            "jdbc:postgresql://db.example.invalid/gestion", " ", "staging-password"
        ));
        assertThrows(IllegalStateException.class, () -> StagingPostgresConfiguration.validateDatasource(
            "jdbc:postgresql://db.example.invalid/gestion", "staging-user", ""
        ));
    }

    @Test
    void rejectsEffectiveDatasourceOverridesAndUnsafeSchemaModes() {
        assertThrows(IllegalStateException.class, () -> StagingPostgresConfiguration.validateDatasource(
            "jdbc:postgresql://approved.example.invalid/gestion", "staging-user", "secret",
            "jdbc:postgresql://other.example.invalid/gestion", "staging-user", "secret",
            "org.postgresql.Driver", "never", "validate", "true", "false", false));
        assertThrows(IllegalStateException.class, () -> StagingPostgresConfiguration.validateDatasource(
            "jdbc:postgresql://approved.example.invalid/gestion", "staging-user", "secret",
            "jdbc:postgresql://approved.example.invalid/gestion", "staging-user", "secret",
            "org.postgresql.Driver", "always", "update", "false", "true", false));
        assertThrows(IllegalStateException.class, () -> StagingPostgresConfiguration.validateDatasource(
            "jdbc:postgresql://approved.example.invalid/gestion", "staging-user", "secret",
            "jdbc:postgresql://approved.example.invalid/gestion", "staging-user", "secret",
            "org.postgresql.Driver", "never", "validate", "true", "false", true));
    }
}
