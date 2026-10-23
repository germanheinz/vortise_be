package com.vortise.gestion.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Fail-closed gates for features that are unsafe without tenant-owned data. */
@Component
public class ProductionFeaturePolicy {
    private final Environment environment;
    private final boolean syntheticData;
    private final boolean isolatedStorage;
    private final boolean gastosFijosEnabled;
    private final boolean demoResetEnabled;

    public ProductionFeaturePolicy(Environment environment,
            @Value("${app.environment.synthetic-data:false}") boolean syntheticData,
            @Value("${app.environment.storage-isolated:false}") boolean isolatedStorage,
            @Value("${app.features.gastos-fijos-enabled:false}") boolean gastosFijosEnabled,
            @Value("${app.demo.reset-enabled:false}") boolean demoResetEnabled) {
        this.environment = environment;
        this.syntheticData = syntheticData;
        this.isolatedStorage = isolatedStorage;
        this.gastosFijosEnabled = gastosFijosEnabled;
        this.demoResetEnabled = demoResetEnabled;
        validateConfiguration();
    }

    public boolean gastosFijosEnabled() {
        return !isProduction() && syntheticData && isolatedStorage && gastosFijosEnabled;
    }

    public boolean demoResetEnabled() {
        return !isProduction() && !isStaging() && isDemoProfile() && syntheticData && isolatedStorage && demoResetEnabled;
    }

    private boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of("prod", "production"));
    }

    private boolean isStaging() {
        return environment.acceptsProfiles(Profiles.of("staging"));
    }

    private boolean isDemoProfile() {
        return environment.acceptsProfiles(Profiles.of("demo"));
    }

    private void validateConfiguration() {
        if (isProduction() && (gastosFijosEnabled || demoResetEnabled)) {
            throw new IllegalStateException("Production profile cannot enable global fixed costs or demo reset");
        }
        if (isStaging() && demoResetEnabled) {
            throw new IllegalStateException("Staging profile cannot enable demo reset");
        }
        if ((gastosFijosEnabled || demoResetEnabled) && (!syntheticData || !isolatedStorage)) {
            throw new IllegalStateException("Demo-only features require explicitly isolated storage with synthetic data");
        }
        if (demoResetEnabled && !isDemoProfile()) {
            throw new IllegalStateException("Demo reset can only be enabled with the demo profile");
        }
    }
}
