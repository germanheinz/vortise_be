package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.infrastructure.backup.H2LogicalBackupService;
import com.vortise.gestion.infrastructure.web.AdminBackupController;
import com.vortise.gestion.infrastructure.web.AdminBackupDisabledController;
import com.vortise.gestion.infrastructure.web.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import static org.junit.jupiter.api.Assertions.*;

/** Configuration-only tests: no Boot context, datasource, application runners or network. */
class ProductionConfigurationTest {
    @Test
    void deployedProfilesExcludeDemoAndH2EvenWhenCombinedWithLocalProfiles() {
        for (String profile : List.of("staging", "prod", "production")) {
            var environment = new MockEnvironment();
            environment.setActiveProfiles(profile, "demo", "local");
            for (Class<?> type : List.of(DatabaseInitializationPolicy.class, DataSeederRunner.class,
                    UsuarioDemoSeeder.class, H2LogicalBackupService.class, AdminBackupController.class)) {
                assertFalse(environment.acceptsProfiles(Profiles.of(type.getAnnotation(Profile.class).value())), type.getName());
            }
            for (Class<?> type : List.of(StagingSeedRunPolicy.class, StagingPostgresConfiguration.class,
                    AdminBackupDisabledController.class)) {
                assertTrue(environment.acceptsProfiles(Profiles.of(type.getAnnotation(Profile.class).value())), type.getName());
            }
        }
    }

    @Test
    void productionDefaultsAndOverridesAreCheckedBeforeAnyDatasourceCanBeCreated() throws Exception {
        for (String profile : List.of("prod", "production")) {
            var loader = new org.springframework.boot.env.YamlPropertySourceLoader();
            var resource = new org.springframework.core.io.ClassPathResource("application-" + profile + ".yml");
            var environment = new MockEnvironment();
            environment.getPropertySources().addLast(loader.load("production", resource).get(0));
            assertThrows(RuntimeException.class, () -> check(environment));
            environment.withProperty("APP_DATABASE_JDBC_URL", "jdbc:postgresql://db.example.invalid/gestion")
                .withProperty("APP_DATABASE_USERNAME", "fixture-user")
                .withProperty("APP_DATABASE_PASSWORD", "fixture-password");
            assertDoesNotThrow(() -> check(environment));
            environment.withProperty("spring.datasource.url", "jdbc:h2:file:./forbidden");
            assertThrows(IllegalStateException.class, () -> check(environment));
            environment.withProperty("spring.datasource.url", "jdbc:postgresql://db.example.invalid/gestion")
                .withProperty("spring.jpa.hibernate.ddl-auto", "update");
            assertThrows(IllegalStateException.class, () -> check(environment));
        }
    }

    @Test
    void rejectsIndependentMigrationConnectionsAndSchemaGenerationOverrides() throws Exception {
        for (String key : List.of("spring.flyway.url", "spring.flyway.user", "spring.flyway.password",
                "spring.jpa.properties.jakarta.persistence.schema-generation.database.action",
                "spring.jpa.properties.javax.persistence.schema-generation.database.action",
                "spring.jpa.properties.hibernate.hbm2ddl.auto",
                "jakarta.persistence.schema-generation.database.action",
                "javax.persistence.schema-generation.database.action", "hibernate.hbm2ddl.auto")) {
            var environment = new MockEnvironment();
            var loader = new org.springframework.boot.env.YamlPropertySourceLoader();
            environment.getPropertySources().addLast(loader.load("production",
                new org.springframework.core.io.ClassPathResource("application-production.yml")).get(0));
            environment.withProperty("APP_DATABASE_JDBC_URL", "jdbc:postgresql://db.example.invalid/gestion")
                .withProperty("APP_DATABASE_USERNAME", "fixture-user")
                .withProperty("APP_DATABASE_PASSWORD", "fixture-password");
            assertDoesNotThrow(() -> check(environment));
            environment.withProperty(key, "unapproved-override");
            assertThrows(IllegalStateException.class, () -> check(environment), key);
        }
    }

    @Test
    void unexpectedErrorsNeverExposeTheirInternalMessage() {
        var response = new GlobalExceptionHandler().handleGeneral(new RuntimeException("jdbc:private internal detail"));
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().error());
        assertFalse(response.getBody().message().contains("private"));
    }

    private void check(MockEnvironment environment) {
        StagingPostgresConfiguration.stagingPostgresConfigurationGuard(environment)
            .postProcessBeanFactory(new DefaultListableBeanFactory());
    }
}
