package com.vortise.gestion.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClerkConfig {
    @Value("${app.clerk.enabled:false}")
    public boolean clerkEnabled;
    
    @Value("${app.clerk.secret-key:}")
    public String secretKey;
}
