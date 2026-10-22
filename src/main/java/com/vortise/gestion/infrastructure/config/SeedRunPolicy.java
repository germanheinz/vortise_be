package com.vortise.gestion.infrastructure.config;

@FunctionalInterface
public interface SeedRunPolicy {
    boolean shouldRunSeeders();
}
