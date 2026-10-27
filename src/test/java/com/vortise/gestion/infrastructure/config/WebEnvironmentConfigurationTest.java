package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class WebEnvironmentConfigurationTest {
    @Test void localDefaultsAreLoopbackOnly() {
        assertEquals(List.of("http://localhost:3000", "http://127.0.0.1:3000"),
            WebEnvironmentConfiguration.validate(false, null, null));
    }
    @Test void acceptsExplicitMatchingOriginsAndNormalizesTrailingSlash() {
        assertEquals(List.of("https://app.example.invalid"), WebEnvironmentConfiguration.validate(true,
            " https://app.example.invalid/,https://app.example.invalid ", "https://app.example.invalid/"));
    }
    @Test void allDeployedProfilesRequireConfiguration() {
        for (String profile : List.of("staging", "prod", "production")) {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles(profile);
            assertThrows(IllegalStateException.class, () -> new WebEnvironmentConfiguration(env));
        }
    }
    @Test void rejectsMissingWildcardLocalAndMalformedOriginsWithoutEchoingValues() {
        for (String origin : List.of("", " ", "*", "https://*.vercel.app", "http://app.example.invalid",
            "https://localhost:3000", "https://127.0.0.1", "https://[::1]", "https://foo.localhost",
            "https://app.example.invalid/login", "https://app.example.invalid?x=1",
            "https://user:password@app.example.invalid", "https://app.example.invalid#fragment",
            "https://app.example.invalid,", "https://app.example.invalid:99999")) {
            var failure = assertThrows(IllegalStateException.class, () -> WebEnvironmentConfiguration.validate(true, origin, "https://app.example.invalid"));
            assertFalse(failure.getMessage().contains("password"));
        }
    }
    @Test void rejectsMissingOrMismatchedFrontend() {
        for (String frontend : new String[]{null, "", "http://localhost:3000", "https://other.example.invalid", "https://app.example.invalid/login"}) {
            assertThrows(IllegalStateException.class, () -> WebEnvironmentConfiguration.validate(true, "https://app.example.invalid", frontend));
        }
    }
}
