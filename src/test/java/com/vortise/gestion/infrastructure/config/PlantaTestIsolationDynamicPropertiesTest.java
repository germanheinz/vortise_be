package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/** Minimal Spring context: no application scanning, auto-configuration or database. */
@SpringJUnitConfig(PlantaTestIsolationConfiguration.class)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlantaTestIsolationDynamicPropertiesTest {
    private static final Map<String, Object> PROPERTIES = PlantaTestIsolationPolicyTest.validProperties();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        PROPERTIES.forEach((key, value) -> registry.add(key, () -> value));
    }

    @Autowired ApplicationContext context;

    @Test void dynamicPropertiesAreAvailableBeforeGuardExecutes() {
        assertTrue(context.containsBean("plantaTestIsolationGuard"));
        assertEquals(PROPERTIES.get("spring.datasource.url"),
            context.getEnvironment().getProperty("spring.datasource.url"));
        assertFalse(context.containsBean("dataSource"));
        assertFalse(context.containsBean("entityManagerFactory"));
    }
}
