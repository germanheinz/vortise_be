package com.vortise.gestion.infrastructure.config;

import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/** Pure configuration inspection: never loads a driver, creates beans or connects. */
public final class PlantaTestIsolationPolicy {
    private static final Pattern URL = Pattern.compile(
        "jdbc:h2:mem:planta_auth_[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
        + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

    private PlantaTestIsolationPolicy() { }

    public static void validate(Environment environment) {
        try {
            validateBound(Binder.get(environment));
        } catch (RuntimeException rejected) {
            // Binding exceptions can contain supplied secrets/URLs. Do not retain
            // their messages or causes in the startup exception.
            throw new IllegalStateException("Planta test isolation configuration rejected");
        }
    }

    private static void validateBound(Binder binder) {
        String url = required(binder, "app.test-isolation.datasource-url");
        require(URL.matcher(url).matches());
        require(url.equals(required(binder, "spring.datasource.url")));
        String secret = required(binder, "app.test-isolation.jwt-secret");
        byte[] decoded = Base64.getDecoder().decode(secret);
        require(decoded.length == 64 && Base64.getEncoder().encodeToString(decoded).equals(secret));
        require(secret.equals(required(binder, "app.security.jwt-secret")));
        require("org.h2.Driver".equals(required(binder, "spring.datasource.driver-class-name")));
        require("never".equals(required(binder, "spring.sql.init.mode")));
        require("false".equals(required(binder, "spring.h2.console.enabled")));
        require("false".equals(required(binder, "spring.jpa.show-sql")));
        require("create-drop".equals(required(binder, "spring.jpa.hibernate.ddl-auto")));

        // Narrow allowlists also reject pool-specific URLs, init SQL, JNDI,
        // datasource classes, provider properties and schema-generation routes.
        allowOnly(binder, "spring.datasource", Set.of("url", "driverclassname", "username", "password"));
        allowOnly(binder, "spring.jpa", Set.of("hibernate.ddlauto", "showsql", "databaseplatform", "openinview"));
        String dialect = binder.bind("spring.jpa.database-platform", String.class).orElse("org.hibernate.dialect.H2Dialect");
        require("org.hibernate.dialect.H2Dialect".equals(dialect));
        allowOnly(binder, "hibernate", Set.of());
        allowOnly(binder, "jakarta.persistence", Set.of());
        allowOnly(binder, "javax.persistence", Set.of());
    }

    private static String required(Binder binder, String key) {
        String value = binder.bind(key, String.class).orElse("");
        require(!value.isBlank() && !value.contains("${"));
        return value;
    }

    private static void allowOnly(Binder binder, String prefix, Set<String> allowed) {
        Map<String, String> properties = binder.bind(prefix, Bindable.mapOf(String.class, String.class))
            .orElse(Map.of());
        for (String key : properties.keySet()) {
            String normalized = key.replace("-", "").replace("_", "").toLowerCase(java.util.Locale.ROOT);
            require(allowed.contains(normalized));
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException();
    }
}
