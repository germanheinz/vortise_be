package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import com.vortise.gestion.infrastructure.security.TenantAccessInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class CorsConfigTest {
    @Test void securityCorsUsesExactConfiguredOriginsOnly() {
        MockEnvironment env = new MockEnvironment().withProperty("APP_ALLOWED_ORIGINS", "https://app.example.invalid")
            .withProperty("APP_FRONTEND_URL", "https://app.example.invalid");
        env.setActiveProfiles("staging");
        var config = new CorsConfig(mock(TenantAccessInterceptor.class), new WebEnvironmentConfiguration(env));
        var cors = config.corsConfigurationSource().getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/proyectos"));
        assertNotNull(cors);
        assertEquals("https://app.example.invalid", cors.checkOrigin("https://app.example.invalid"));
        assertNull(cors.checkOrigin("https://unrelated.vercel.app"));
        assertNull(cors.checkOrigin("http://localhost:3000"));
        assertNull(cors.getAllowedOriginPatterns());
        assertFalse(cors.getAllowCredentials());
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertTrue(cors.getAllowedMethods().contains("PATCH"));
    }
}
