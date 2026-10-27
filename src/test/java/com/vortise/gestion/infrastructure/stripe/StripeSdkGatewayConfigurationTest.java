package com.vortise.gestion.infrastructure.stripe;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StripeSdkGatewayConfigurationTest {
    @Test void requiresBothSecretsWithoutNetworkRequests() {
        for (String absent : new String[]{null, "", "   "}) {
            assertFalse(new StripeSdkGateway(absent, "whsec_fixture_not_real").isConfigured());
            assertFalse(new StripeSdkGateway("sk_test_fixture_not_real", absent).isConfigured());
        }
        assertTrue(new StripeSdkGateway(" sk_test_fixture_not_real ", " whsec_fixture_not_real ").isConfigured());
    }
}
