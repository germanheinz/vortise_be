package com.vortise.gestion.infrastructure.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Additional pre-DataSource boundary for the disposable browser test harness. */
@Configuration(proxyBeanMethods = false)
@Profile("e2e-local")
public class E2eLocalConfiguration {
    @Bean
    static BeanFactoryPostProcessor e2eLocalGuard(Environment environment) {
        return factory -> validate(environment);
    }

    static void validate(Environment env) {
        try {
            require(Arrays.equals(env.getActiveProfiles(), new String[]{"e2e-local"}));
            require("jdbc:postgresql://127.0.0.1:15435/e2e10_test".equals(env.getRequiredProperty("APP_DATABASE_JDBC_URL")));
            require("e2e10_test".equals(env.getRequiredProperty("APP_DATABASE_USERNAME")));
            require(env.getRequiredProperty("APP_DATABASE_PASSWORD").length() >= 32);
            require(env.getRequiredProperty("APP_E2E_TEAM_PASSWORD").length() >= 32);
            require("127.0.0.1".equals(env.getRequiredProperty("server.address")));
            require("18081".equals(env.getRequiredProperty("server.port")));
            require("http://127.0.0.1:3001".equals(env.getRequiredProperty("APP_ALLOWED_ORIGINS")));
            require("http://127.0.0.1:3001".equals(env.getRequiredProperty("APP_FRONTEND_URL")));
            String uploads = env.getRequiredProperty("APP_E2E_UPLOADS_DIR");
            require(uploads.matches("/(?:private/)?tmp/e2e10-[A-Za-z0-9_-]+/uploads"));
            require(uploads.equals(env.getRequiredProperty("app.uploads.dir")));
            for (String key : List.of("app.password-mail.enabled", "app.demo.reset-enabled", "app.features.gastos-fijos-enabled")) {
                require("false".equals(env.getRequiredProperty(key)));
            }
            for (String key : List.of("app.superadmin.email", "app.superadmin.password", "app.stripe.secret-key",
                    "app.stripe.webhook-secret", "app.stripe.price-basic", "app.stripe.price-pro")) {
                require("".equals(env.getRequiredProperty(key)));
            }
            require(env.getProperty("spring.datasource.hikari.connection-init-sql") == null);
        } catch (RuntimeException rejected) {
            // Never attach binding errors which can contain URLs or secrets.
            throw new IllegalStateException("E2E local isolation configuration rejected");
        }
    }

    private static void require(boolean valid) {
        if (!valid) throw new IllegalArgumentException();
    }
}
