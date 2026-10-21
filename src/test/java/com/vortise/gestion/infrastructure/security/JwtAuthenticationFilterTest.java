package com.vortise.gestion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vortise.gestion.application.usecase.JwtService;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
    private final JwtService jwtService = new JwtService("test-only-signing-key-with-enough-length-1234567890", 60_000);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void regularTokenGrantsOnlyItsRole() throws Exception {
        assertEquals(Set.of("ROLE_ADMINISTRADOR"), authoritiesFor(jwtService.generate(1L, 2L, "a@example.invalid", "ADMINISTRADOR")));
    }

    @Test
    void superAdminTokenAddsSuperAdminAuthority() throws Exception {
        assertEquals(Set.of("ROLE_ADMINISTRADOR", "ROLE_SUPER_ADMIN"),
            authoritiesFor(jwtService.generate(1L, 2L, "a@example.invalid", "ADMINISTRADOR", true)));
    }

    private Set<String> authoritiesFor(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
