package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.application.port.FeaturePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Fail-closed gates for features that are unsafe without tenant-owned data. */
@Component
public class ProductionFeaturePolicy implements FeaturePolicy {
    private final Environment environment;
    private final boolean syntheticData;
    private final boolean isolatedStorage;
    private final boolean gastosFijosEnabled;

    public ProductionFeaturePolicy(Environment environment,
            @Value("${app.environment.synthetic-data:false}") boolean syntheticData,
            @Value("${app.environment.storage-isolated:false}") boolean isolatedStorage,
            @Value("${app.features.gastos-fijos-enabled:false}") boolean gastosFijosEnabled) {
        this.environment = environment;
        this.syntheticData = syntheticData;
        this.isolatedStorage = isolatedStorage;
        this.gastosFijosEnabled = gastosFijosEnabled;
        validateConfiguration();
    }

    public boolean gastosFijosEnabled() {
        return !isProduction() && syntheticData && isolatedStorage && gastosFijosEnabled;
    }

    private boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of("prod", "production"));
    }

    private void validateConfiguration() {
        if (isProduction() && gastosFijosEnabled) {
            throw new IllegalStateException("Production profile cannot enable global fixed costs");
        }
        if (gastosFijosEnabled && (!syntheticData || !isolatedStorage)) {
            throw new IllegalStateException("Demo-only features require explicitly isolated storage with synthetic data");
        }
    }
}
