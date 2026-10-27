package com.vortise.gestion.infrastructure.config;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Validates public web origins without logging configuration values. */
@Configuration
public class WebEnvironmentConfiguration {
    private final List<String> allowedOrigins;

    public WebEnvironmentConfiguration(Environment environment) {
        boolean deployed = environment.acceptsProfiles(Profiles.of("staging", "prod", "production"));
        allowedOrigins = validate(deployed, environment.getProperty("APP_ALLOWED_ORIGINS"),
            environment.getProperty("APP_FRONTEND_URL"));
    }

    public List<String> allowedOrigins() {
        return allowedOrigins;
    }

    static List<String> validate(boolean deployed, String origins, String frontendUrl) {
        if (origins == null || origins.isBlank()) {
            if (deployed) throw new IllegalStateException("APP_ALLOWED_ORIGINS is required in deployed environments");
            return List.of("http://localhost:3000", "http://127.0.0.1:3000");
        }
        List<String> allowed = Arrays.stream(origins.split(",", -1))
            .map(value -> origin(value, deployed, "APP_ALLOWED_ORIGINS")).distinct().toList();
        if (deployed && !allowed.contains(origin(frontendUrl, true, "APP_FRONTEND_URL"))) {
            throw new IllegalStateException("APP_FRONTEND_URL must match an allowed origin");
        }
        return allowed;
    }

    private static String origin(String value, boolean deployed, String setting) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            String host = uri.getHost();
            boolean loopback = host != null && (host.equalsIgnoreCase("localhost")
                || host.toLowerCase(java.util.Locale.ROOT).endsWith(".localhost")
                || host.startsWith("127.") || host.equals("[::1]"));
            if (host == null || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !uri.getRawPath().equals("/"))
                || uri.getPort() == 0 || uri.getPort() > 65535
                || !("https".equals(uri.getScheme()) || (!deployed && "http".equals(uri.getScheme())))
                || (deployed && loopback)) throw new IllegalArgumentException();
            // Match the actual browser Origin format: no trailing slash.
            return uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(setting + " must contain exact web origins; deployed origins require HTTPS and no loopback");
        }
    }
}
