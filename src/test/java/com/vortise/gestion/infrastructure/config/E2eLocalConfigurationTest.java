package com.vortise.gestion.infrastructure.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

/** Pure environment checks; never opens a datasource or starts application beans. */
class E2eLocalConfigurationTest {
    private MockEnvironment valid() throws Exception {
        var env = new MockEnvironment();
        env.setActiveProfiles("e2e-local");
        var source = new org.springframework.boot.env.YamlPropertySourceLoader().load("e2e",
            new org.springframework.core.io.ClassPathResource("application-e2e-local.yml")).get(0);
        env.getPropertySources().addLast(source);
        return env.withProperty("APP_DATABASE_JDBC_URL", "jdbc:postgresql://127.0.0.1:15435/e2e10_test")
            .withProperty("APP_DATABASE_USERNAME", "e2e10_test")
            .withProperty("APP_DATABASE_PASSWORD", "synthetic-configuration-test-only-password")
            .withProperty("APP_E2E_TEAM_PASSWORD", "synthetic-team-configuration-only-password")
            .withProperty("APP_ALLOWED_ORIGINS", "http://127.0.0.1:3001")
            .withProperty("APP_E2E_UPLOADS_DIR", "/private/tmp/e2e10-configuration-test/uploads")
            .withProperty("APP_FRONTEND_URL", "http://127.0.0.1:3001");
    }

    @Test void acceptsOnlyTheDisposableLocalContract() throws Exception {
        var env = valid();
        assertDoesNotThrow(() -> E2eLocalConfiguration.validate(env));
        assertDoesNotThrow(() -> StagingPostgresConfiguration.stagingPostgresConfigurationGuard(env)
            .postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
    }

    @Test void acceptsLinuxTemporaryDirectoryAndRejectsEscapes() throws Exception {
        for (String path : new String[]{"/tmp/e2e10-ci-test/uploads", "/private/tmp/e2e10-macos-test/uploads"}) {
            var env = valid().withProperty("APP_E2E_UPLOADS_DIR", path).withProperty("app.uploads.dir", path);
            assertDoesNotThrow(() -> E2eLocalConfiguration.validate(env));
        }
        for (String path : new String[]{"/tmp/uploads", "/tmp/e2e10-ci/../uploads",
                "/tmp/e2e10-ci/uploads/", "/home/runner/e2e10-ci/uploads", "/tmp/e2e10-/uploads"}) {
            var env = valid().withProperty("APP_E2E_UPLOADS_DIR", path).withProperty("app.uploads.dir", path);
            assertThrows(IllegalStateException.class, () -> E2eLocalConfiguration.validate(env));
        }
    }

    @Test void rejectsOverridesWithoutCreatingFactories() throws Exception {
        for (var override : Map.ofEntries(
                Map.entry("APP_DATABASE_JDBC_URL", "jdbc:postgresql://127.0.0.1:15433/other"),
                Map.entry("APP_E2E_TEAM_PASSWORD", "short"), Map.entry("APP_DATABASE_USERNAME", "other"), Map.entry("APP_DATABASE_PASSWORD", "short"),
                Map.entry("server.address", "0.0.0.0"), Map.entry("server.port", "8080"),
                Map.entry("APP_ALLOWED_ORIGINS", "https://external.example.invalid"),
                Map.entry("APP_E2E_UPLOADS_DIR", "./data/uploads/avatars"),
                Map.entry("app.uploads.dir", "./data/uploads/avatars"),
                Map.entry("app.password-mail.enabled", "true"), Map.entry("app.stripe.secret-key", "synthetic-invalid"),
                Map.entry("app.superadmin.email", "fixture@example.invalid"),
                Map.entry("spring.datasource.hikari.connection-init-sql", "SELECT 1")).entrySet()) {
            var env = valid().withProperty(override.getKey(), override.getValue());
            var failure = assertThrows(IllegalStateException.class, () -> E2eLocalConfiguration.validate(env));
            assertEquals("E2E local isolation configuration rejected", failure.getMessage());
            assertNull(failure.getCause());
        }
        var mixed = valid(); mixed.setActiveProfiles("e2e-local", "production");
        assertThrows(IllegalStateException.class, () -> E2eLocalConfiguration.validate(mixed));
    }
}
