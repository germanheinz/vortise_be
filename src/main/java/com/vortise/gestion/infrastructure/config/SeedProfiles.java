package com.vortise.gestion.infrastructure.config;

/**
 * Profiles that may run the demonstration seeders. Seeding is opt-in: a start with no profile, or with
 * any deployed profile, never creates demo companies, users or credentials.
 */
final class SeedProfiles {
    static final String DEMO_ONLY =
        "(demo | local | payments-local | planta-authorization-test) & !staging & !prod & !production & !e2e-local";

    private SeedProfiles() { }
}
