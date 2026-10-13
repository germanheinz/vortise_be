package com.vortise.gestion.infrastructure.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Discovered by application scanning, including launches without test resources. */
@Configuration(proxyBeanMethods = false)
@Profile("planta-authorization-test")
public class PlantaTestIsolationConfiguration {
    @Bean
    static BeanFactoryPostProcessor plantaTestIsolationGuard(Environment environment) {
        // No bean lookup: dynamic test properties are installed before refresh,
        // and ordinary DataSource/JPA singletons have not been instantiated yet.
        return beanFactory -> PlantaTestIsolationPolicy.validate(environment);
    }
}
