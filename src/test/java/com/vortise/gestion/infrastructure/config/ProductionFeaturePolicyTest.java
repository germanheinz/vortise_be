package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionFeaturePolicyTest {
    @Test
    void featuresAreDisabledByDefault() {
        ProductionFeaturePolicy policy = policy(new MockEnvironment(), false, false, false);

        assertFalse(policy.gastosFijosEnabled());
    }

    @Test
    void syntheticOptInEnablesFixedCostsOutsideProduction() {
        ProductionFeaturePolicy policy = policy(new MockEnvironment(), true, true, true);

        assertTrue(policy.gastosFijosEnabled());
    }

    @Test
    void rejectsEnablingSyntheticFeaturesWithoutSyntheticDataOptIn() {
        assertThrows(IllegalStateException.class,
            () -> policy(new MockEnvironment(), true, false, true));
    }

    @Test
    void productionAlwaysDisablesFeaturesAndRejectsExplicitEnablement() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");
        ProductionFeaturePolicy disabled = policy(production, false, false, false);
        assertFalse(disabled.gastosFijosEnabled());

        assertThrows(IllegalStateException.class,
            () -> {
                MockEnvironment prodEnvironment = new MockEnvironment();
                prodEnvironment.setActiveProfiles("prod");
                policy(prodEnvironment, true, true, true);
            });
    }

    private static ProductionFeaturePolicy policy(MockEnvironment environment, boolean synthetic, boolean isolated,
            boolean gastosFijos) {
        return new ProductionFeaturePolicy(environment, synthetic, isolated, gastosFijos);
    }
}
