package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AssignableTypeFilter;

/** No Boot auto-configuration, drivers, real DataSource, JPA or runners. */
class PlantaTestIsolationLifecycleTest {
    private AnnotationConfigApplicationContext context(boolean active, Map<String, Object> properties,
                                                       AtomicInteger creations) {
        var context = new AnnotationConfigApplicationContext();
        context.setEnvironment(PlantaTestIsolationPolicyTest.environment(properties));
        if (active) context.getEnvironment().setActiveProfiles("planta-authorization-test");
        // Exercise component discovery rather than importing the guard on the HTTP class.
        var scanner = new ClassPathBeanDefinitionScanner(context, false);
        scanner.addIncludeFilter(new AssignableTypeFilter(PlantaTestIsolationConfiguration.class));
        scanner.scan("com.vortise.gestion.infrastructure.config");
        context.registerBean("dataSource", Object.class, () -> { creations.incrementAndGet(); return new Object(); });
        context.registerBean("entityManagerFactory", Object.class, () -> { creations.incrementAndGet(); return new Object(); });
        return context;
    }

    @Test void standaloneWithoutTestYamlFailsBeforeFactories() {
        var creations = new AtomicInteger();
        try (var context = context(true, Map.of(), creations)) {
            assertRejected(context, creations);
        }
    }

    @Test void actualProfileWithoutExplicitContractFailsBeforeFactories() throws Exception {
        var creations = new AtomicInteger();
        try (var context = context(true, Map.of(), creations)) {
            for (var source : new YamlPropertySourceLoader().load("profile",
                    new ClassPathResource("application-planta-authorization-test.yml"))) {
                context.getEnvironment().getPropertySources().addLast(source);
            }
            assertRejected(context, creations);
        }
    }

    @Test void unsafeHigherPriorityOverrideFailsBeforeFactories() {
        var creations = new AtomicInteger();
        try (var context = context(true, PlantaTestIsolationPolicyTest.validProperties(), creations)) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("override",
                Map.of("spring.datasource.url", "jdbc:h2:file:./never-open")));
            assertRejected(context, creations);
        }
    }

    @Test void validExplicitContractPermitsOnlySentinelFactories() {
        var creations = new AtomicInteger();
        try (var context = context(true, PlantaTestIsolationPolicyTest.validProperties(), creations)) {
            context.refresh();
            assertEquals(2, creations.get());
        }
    }

    @Test void inactiveProfileLeavesOrdinaryBehaviorUntouched() {
        var creations = new AtomicInteger();
        try (var context = context(false, Map.of(), creations)) {
            context.refresh();
            assertFalse(context.containsBean("plantaTestIsolationGuard"));
            assertEquals(2, creations.get());
        }
    }

    private void assertRejected(AnnotationConfigApplicationContext context, AtomicInteger creations) {
        var error = assertThrows(IllegalStateException.class, context::refresh);
        assertEquals("Planta test isolation configuration rejected", error.getMessage());
        assertEquals(0, creations.get());
    }
}
