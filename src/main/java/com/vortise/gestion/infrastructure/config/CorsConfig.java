package com.vortise.gestion.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.vortise.gestion.infrastructure.security.TenantAccessInterceptor;
import org.springframework.lang.NonNull;

@Configuration
public class CorsConfig {
    @NonNull
    private final TenantAccessInterceptor tenantAccessInterceptor;

    private final WebEnvironmentConfiguration webEnvironment;

    public CorsConfig(@NonNull TenantAccessInterceptor tenantAccessInterceptor, WebEnvironmentConfiguration webEnvironment) {
        this.tenantAccessInterceptor = tenantAccessInterceptor;
        this.webEnvironment = webEnvironment;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer(){

            @Override
            public void addInterceptors(@NonNull org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                registry.addInterceptor(tenantAccessInterceptor).addPathPatterns("/api/empresas/**", "/api/proyectos/**", "/api/restricciones/**", "/api/registro-horas/**", "/api/tareas/**");
            }

        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(webEnvironment.allowedOrigins());
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
