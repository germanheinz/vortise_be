package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class PaymentConfigurationControllerTest {
    @Test void reportsPresenceAndModeWithoutReturningSecrets() throws Exception {
        var dto = new PaymentConfigurationController("sk_test_fixture", "whsec_fixture", "price_basic_fixture", "price_pro_fixture").configuration();
        assertTrue(dto.apiKeyPresent()); assertTrue(dto.webhookSecretPresent());
        assertTrue(dto.basicPricePresent()); assertTrue(dto.proPricePresent());
        assertTrue(dto.paymentsConfigured()); assertEquals("test", dto.mode());
        String json = tools.jackson.databind.json.JsonMapper.builder().findAndAddModules().build().writeValueAsString(dto);
        assertFalse(json.contains("fixture")); assertFalse(json.contains("sk_test_"));
    }
    @Test void missingConfigurationAndUnknownModeAreExplicit() {
        var dto = new PaymentConfigurationController(null, " ", "", null).configuration();
        assertFalse(dto.paymentsConfigured()); assertFalse(dto.basicPricePresent());
        assertFalse(dto.proPricePresent()); assertEquals("desconocido", dto.mode());
        assertEquals("live", new PaymentConfigurationController("rk_live_fixture", "x", "x", "x").configuration().mode());
        assertFalse(new PaymentConfigurationController("sk_test_fixture", "", "x", "x").configuration().paymentsConfigured());
    }
    @Test void controllerRequiresExactlySuperAdmin() {
        assertEquals("hasAuthority('ROLE_SUPER_ADMIN')", PaymentConfigurationController.class.getAnnotation(PreAuthorize.class).value());
    }
}
