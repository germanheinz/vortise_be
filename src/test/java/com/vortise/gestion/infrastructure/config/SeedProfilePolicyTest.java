package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Profiles;
import org.springframework.mock.env.MockEnvironment;

/** Configuration-only: no Boot context, datasource or runners. */
class SeedProfilePolicyTest {
    private static final List<Class<?>> DEMO_SEEDERS = List.of(UsuarioDemoSeeder.class, DataSeederRunner.class);

    private static boolean active(Class<?> type, String... profiles) {
        var environment = new MockEnvironment();
        if (profiles.length > 0) environment.setActiveProfiles(profiles);
        return environment.acceptsProfiles(Profiles.of(type.getAnnotation(Profile.class).value()));
    }

    @Test
    void startWithoutAnyProfileCreatesNoDemoData() {
        for (Class<?> seeder : DEMO_SEEDERS) assertFalse(active(seeder), seeder.getName());
    }

    @Test
    void anUnrelatedProfileDoesNotEnableDemoData() {
        for (Class<?> seeder : DEMO_SEEDERS) assertFalse(active(seeder, "someother"), seeder.getName());
    }

    @Test
    void explicitDemoAndLocalProfilesEnableDemoData() {
        for (Class<?> seeder : DEMO_SEEDERS) {
            for (String profile : List.of("demo", "local", "payments-local", "planta-authorization-test")) {
                assertTrue(active(seeder, profile), seeder.getName() + " / " + profile);
            }
        }
    }

    @Test
    void deployedAndE2eProfilesWinOverAnyOptIn() {
        for (Class<?> seeder : DEMO_SEEDERS) {
            for (String deployed : List.of("staging", "prod", "production", "e2e-local")) {
                assertFalse(active(seeder, deployed, "demo", "local"), seeder.getName() + " / " + deployed);
            }
        }
    }
}
