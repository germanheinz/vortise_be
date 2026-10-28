package com.vortise.gestion.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Prevents automatic demo/business fixture seeders from running against deployed PostgreSQL. */
@Component
@Profile({"staging", "prod", "production", "e2e-local"})
public class StagingSeedRunPolicy implements SeedRunPolicy {
    @Override
    public boolean shouldRunSeeders() {
        return false;
    }
}
