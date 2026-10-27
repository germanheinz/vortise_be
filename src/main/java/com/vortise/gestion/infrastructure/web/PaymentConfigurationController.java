package com.vortise.gestion.infrastructure.web;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Presence diagnostics only: never returns credentials or calls Stripe. */
@RestController
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class PaymentConfigurationController {
    private final boolean apiKeyPresent;
    private final boolean webhookSecretPresent;
    private final boolean basicPricePresent;
    private final boolean proPricePresent;
    private final String mode;

    public PaymentConfigurationController(@Value("${app.stripe.secret-key:}") String key,
            @Value("${app.stripe.webhook-secret:}") String webhook,
            @Value("${app.stripe.price-basic:}") String basic,
            @Value("${app.stripe.price-pro:}") String pro) {
        apiKeyPresent = present(key);
        webhookSecretPresent = present(webhook);
        basicPricePresent = present(basic);
        proPricePresent = present(pro);
        String normalized = key == null ? "" : key.trim();
        mode = normalized.startsWith("sk_test_") || normalized.startsWith("rk_test_") ? "test"
            : normalized.startsWith("sk_live_") || normalized.startsWith("rk_live_") ? "live" : "desconocido";
    }

    @GetMapping("/api/admin/pagos/configuracion")
    public ConfigurationDto configuration() {
        return new ConfigurationDto(apiKeyPresent, webhookSecretPresent, basicPricePresent, proPricePresent,
            apiKeyPresent && webhookSecretPresent, mode, Instant.now());
    }

    private static boolean present(String value) { return value != null && !value.isBlank(); }

    public record ConfigurationDto(boolean apiKeyPresent, boolean webhookSecretPresent,
            boolean basicPricePresent, boolean proPricePresent, boolean paymentsConfigured,
            String mode, Instant checkedAt) { }
}
