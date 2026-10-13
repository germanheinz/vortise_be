package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class PlantaTestIsolationPolicyTest {
    static Map<String, Object> validProperties() {
        var p = new LinkedHashMap<String, Object>();
        String url = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID()
            + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        p.put("app.test-isolation.datasource-url", url);
        p.put("spring.datasource.url", url);
        p.put("app.test-isolation.jwt-secret", secret);
        p.put("app.security.jwt-secret", secret);
        p.put("spring.datasource.driver-class-name", "org.h2.Driver");
        p.put("spring.sql.init.mode", "never");
        p.put("spring.h2.console.enabled", "false");
        p.put("spring.jpa.show-sql", "false");
        p.put("spring.jpa.hibernate.ddl-auto", "create-drop");
        return p;
    }

    static StandardEnvironment environment(Map<String, Object> p) {
        var env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new MapPropertySource("test", p));
        return env;
    }

    @Test void acceptsExplicitContract() {
        assertDoesNotThrow(() -> PlantaTestIsolationPolicy.validate(environment(validProperties())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ISOLATION_CONFIGURATION_REQUIRED", "jdbc:h2:file:./never-open",
        "jdbc:h2:./never-open", "jdbc:h2:~/never-open", "jdbc:h2:tcp://localhost/never-open",
        "jdbc:h2:ssl://localhost/never-open", "jdbc:h2:mem:", "jdbc:h2:mem:planta_auth_not-a-uuid",
        "${unresolved}"})
    void rejectsUrlsWithoutConnecting(String url) {
        var p = validProperties();
        p.put("spring.datasource.url", url);
        p.put("app.test-isolation.datasource-url", url);
        assertThrows(IllegalStateException.class, () -> PlantaTestIsolationPolicy.validate(environment(p)));
    }

    @ParameterizedTest
    @ValueSource(strings = {";INIT=RUNSCRIPT FROM 'never-open'", ";AUTO_SERVER=TRUE",
        ";TRACE_LEVEL_FILE=3", ";IGNORE_UNKNOWN_SETTINGS=TRUE", ";DB_CLOSE_DELAY=-1", ";", " ", "\n"})
    void rejectsAdditionalOptions(String suffix) {
        var p = validProperties();
        String url = p.get("spring.datasource.url") + suffix;
        p.put("spring.datasource.url", url);
        p.put("app.test-isolation.datasource-url", url);
        assertThrows(IllegalStateException.class, () -> PlantaTestIsolationPolicy.validate(environment(p)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"app.test-isolation.datasource-url", "app.test-isolation.jwt-secret",
        "spring.datasource.url", "app.security.jwt-secret", "spring.sql.init.mode",
        "spring.h2.console.enabled", "spring.jpa.hibernate.ddl-auto", "spring.jpa.show-sql"})
    void rejectsMissingRequiredProperty(String key) {
        var p = validProperties();
        p.remove(key);
        assertThrows(IllegalStateException.class, () -> PlantaTestIsolationPolicy.validate(environment(p)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring.datasource.hikari.jdbc-url", "spring.datasource.hikari.connection-init-sql",
        "spring.datasource.hikari.data-source-properties.url", "spring.datasource.jndi-name",
        "spring.datasource.type", "spring.jpa.properties.hibernate.connection.url",
        "spring.jpa.properties.jakarta.persistence.jdbc.url", "spring.jpa.properties.hibernate.connection.provider_class",
        "spring.jpa.properties.jakarta.persistence.schema-generation.connection", "hibernate.connection.url",
        "jakarta.persistence.jdbc.url", "javax.persistence.jdbc.url"})
    void rejectsAlternativeRoutes(String key) {
        var p = validProperties();
        p.put(key, "forbidden");
        assertThrows(IllegalStateException.class, () -> PlantaTestIsolationPolicy.validate(environment(p)));
    }

    @Test void rejectsNonCanonicalNamesAndOptionOrder() {
        var original = validProperties();
        String valid = (String) original.get("spring.datasource.url");
        for (String url : new String[] {
                valid.replace("planta_auth_", "other_"),
                valid.replace(";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", ";DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1"),
                "jdbc:h2:mem:planta_auth_00000000-0000-0000-0000-000000000000;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                " " + valid}) {
            var p = new LinkedHashMap<>(original);
            p.put("spring.datasource.url", url);
            p.put("app.test-isolation.datasource-url", url);
            assertThrows(IllegalStateException.class, () -> PlantaTestIsolationPolicy.validate(environment(p)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring.datasource.driver-class-name", "spring.sql.init.mode",
        "spring.h2.console.enabled", "spring.jpa.show-sql", "spring.jpa.hibernate.ddl-auto",
        "spring.jpa.database-platform", "spring.datasource.url"})
    void rejectsUnsafeEffectiveSettings(String key) {
        var p = validProperties();
        p.put(key, "unsafe");
        assertThrows(IllegalStateException.class, () -> PlantaTestIsolationPolicy.validate(environment(p)));
    }

    @ParameterizedTest
    @ValueSource(strings = {StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
        "override-systemEnvironment"})
    void rejectsRelaxedEnvironmentOverride(String sourceName) {
        var env = environment(validProperties());
        env.getPropertySources().addFirst(new SystemEnvironmentPropertySource(sourceName,
            Map.of("SPRING_DATASOURCE_HIKARI_JDBCURL", "forbidden")));
        var datasourceProperties = Binder.get(env)
            .bind("spring.datasource", Bindable.mapOf(String.class, String.class)).get();
        assertEquals("forbidden", datasourceProperties.get("hikari.jdbcurl"),
            "The simulated environment variable must be visible to the policy's map binding");
        assertThrows(IllegalStateException.class, () -> PlantaTestIsolationPolicy.validate(env));
    }

    @Test void rejectsMismatchAndSanitizesFailure() {
        var p = validProperties();
        p.put("app.security.jwt-secret", "sensitive-value");
        var error = assertThrows(IllegalStateException.class,
            () -> PlantaTestIsolationPolicy.validate(environment(p)));
        assertEquals("Planta test isolation configuration rejected", error.getMessage());
        assertNull(error.getCause());
    }

    @Test void rejectsInvalidKeyEncodingAndSize() {
        for (String key : new String[] {"", "not-base64", Base64.getEncoder().encodeToString(new byte[32])}) {
            var p = validProperties();
            p.put("app.test-isolation.jwt-secret", key);
            p.put("app.security.jwt-secret", key);
            assertThrows(IllegalStateException.class, () -> PlantaTestIsolationPolicy.validate(environment(p)));
        }
    }
}
