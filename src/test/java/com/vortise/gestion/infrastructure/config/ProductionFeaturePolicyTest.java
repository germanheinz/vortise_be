package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionFeaturePolicyTest {
    @Test
    void featuresAreDisabledByDefault() {
        ProductionFeaturePolicy policy = policy(new MockEnvironment(), false, false, false, false);

        assertFalse(policy.gastosFijosEnabled());
        assertFalse(policy.demoResetEnabled());
    }

    @Test
    void syntheticOptInEnablesFixedCostsOutsideProduction() {
        ProductionFeaturePolicy policy = policy(new MockEnvironment(), true, true, true, false);

        assertTrue(policy.gastosFijosEnabled());
        assertFalse(policy.demoResetEnabled());
    }

    @Test
    void demoResetRequiresDemoProfileAndSyntheticData() {
        MockEnvironment demo = new MockEnvironment();
        demo.setActiveProfiles("demo");
        ProductionFeaturePolicy policy = policy(demo, true, true, false, true);

        assertTrue(policy.demoResetEnabled());
        assertFalse(policy.gastosFijosEnabled());
    }

    @Test
    void rejectsDemoResetOutsideDemoProfile() {
        assertThrows(IllegalStateException.class,
            () -> policy(new MockEnvironment(), true, true, false, true));
    }

    @Test
    void rejectsEnablingSyntheticFeaturesWithoutSyntheticDataOptIn() {
        assertThrows(IllegalStateException.class,
            () -> policy(new MockEnvironment(), true, false, true, false));
    }

    @Test
    void productionAlwaysDisablesFeaturesAndRejectsExplicitEnablement() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");
        ProductionFeaturePolicy disabled = policy(production, false, false, false, false);
        assertFalse(disabled.gastosFijosEnabled());
        assertFalse(disabled.demoResetEnabled());

        assertThrows(IllegalStateException.class,
            () -> {
                MockEnvironment prodEnvironment = new MockEnvironment();
                prodEnvironment.setActiveProfiles("prod");
                policy(prodEnvironment, true, true, true, false);
            });
    }

    @Test
    void stagingCannotEnableDemoResetEvenWhenDemoStorageIsIsolated() {
        MockEnvironment stagingDemo = new MockEnvironment();
        stagingDemo.setActiveProfiles("staging", "demo");

        ProductionFeaturePolicy disabled = policy(stagingDemo, true, true, false, false);
        assertFalse(disabled.demoResetEnabled());

        assertThrows(IllegalStateException.class,
            () -> policy(stagingDemo, true, true, false, true));
    }

    private static ProductionFeaturePolicy policy(MockEnvironment environment, boolean synthetic, boolean isolated,
            boolean gastosFijos, boolean reset) {
        return new ProductionFeaturePolicy(environment, synthetic, isolated, gastosFijos, reset);
    }
}
