package com.vortise.gestion.infrastructure.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Runs synthetic fixture seeding only in non-staging environments. */
@Component
@Profile("!staging & !prod & !production")
@Order(Ordered.LOWEST_PRECEDENCE)
public class DataSeederRunner implements CommandLineRunner {
    private final DataSeeder dataSeeder;
    private final SeedRunPolicy seedRunPolicy;

    public DataSeederRunner(DataSeeder dataSeeder, SeedRunPolicy seedRunPolicy) {
        this.dataSeeder = dataSeeder;
        this.seedRunPolicy = seedRunPolicy;
    }

    @Override
    public void run(String... args) {
        if (seedRunPolicy.shouldRunSeeders()) {
            dataSeeder.run(args);
        }
    }
}
