/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.web.servlet.config.annotation.CorsRegistry
 *  org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 */
package com.vortise.gestion.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.vortise.gestion.infrastructure.security.TenantAccessInterceptor;
import org.springframework.lang.NonNull;

@Configuration
public class CorsConfig {
    @NonNull
    private final TenantAccessInterceptor tenantAccessInterceptor;

    public CorsConfig(@NonNull TenantAccessInterceptor tenantAccessInterceptor) {
        this.tenantAccessInterceptor = tenantAccessInterceptor;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer(){

            @Override
            public void addInterceptors(@NonNull org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                registry.addInterceptor(tenantAccessInterceptor).addPathPatterns("/api/empresas/**", "/api/proyectos/**", "/api/restricciones/**", "/api/registro-horas/**", "/api/tareas/**");
            }

            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOriginPatterns("https://*.vercel.app", "http://localhost:3000", "http://127.0.0.1:3000")
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(false);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of(
            "https://*.vercel.app",
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        ));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
