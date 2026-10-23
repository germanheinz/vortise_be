package com.vortise.gestion.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Prevents automatic demo/business fixture seeders from running against staging PostgreSQL. */
@Component
@Profile("staging")
public class StagingSeedRunPolicy implements SeedRunPolicy {
    @Override
    public boolean shouldRunSeeders() {
        return false;
    }
}
