package com.vortise.gestion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = new JwtService("test-only-signing-key-with-enough-length-1234567890", 60_000);
    private final UsuarioEmpresaRepository users = mock(UsuarioEmpresaRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users);
    private UsuarioEmpresa user;
    @BeforeEach void setup() {
        var company = new Empresa("Test", null); ReflectionTestUtils.setField(company, "id", 2L);
        user = new UsuarioEmpresa(company, "test@example.invalid", "Test", "hash", RolEmpresa.CONSULTA);
        ReflectionTestUtils.setField(user, "id", 1L); when(users.findById(1L)).thenReturn(Optional.of(user));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private void authenticate(String token) throws Exception {
        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }
    @Test void usesCurrentRoleInsteadOfStaleClaims() throws Exception {
        authenticate(jwt.generate(1L, 99L, "stale@example.invalid", "ADMINISTRADOR", true, user.getSessionVersion()));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(List.of("ROLE_CONSULTA"), auth.getAuthorities().stream().map(Object::toString).toList());
        assertEquals(2L, ((io.jsonwebtoken.Claims) auth.getDetails()).get("empresaId", Number.class).longValue());
    }
    @Test void currentSuperAdminGetsAuthority() throws Exception {
        user.setSuperAdmin(true);
        authenticate(jwt.generate(1L, 2L, user.getEmail(), "CONSULTA", true, user.getSessionVersion()));
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")));
    }
    @Test void passwordChangeRevokesPreviousToken() throws Exception {
        String token = jwt.generate(1L, 2L, user.getEmail(), "CONSULTA", false, user.getSessionVersion()); user.setPasswordHash("new"); authenticate(token);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void deactivationAndRoleChangeRevokeToken() throws Exception {
        String token = jwt.generate(1L, 2L, user.getEmail(), "CONSULTA", false, user.getSessionVersion());
        user.setActivo(false); authenticate(token); assertNull(SecurityContextHolder.getContext().getAuthentication());
        user.setActivo(true); user.setRol(RolEmpresa.RESPONSABLE); authenticate(token); assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void missingAccountAndMalformedTokenAreUnauthenticated() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.empty()); authenticate(jwt.generate(1L, 2L, "none", "CONSULTA"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        authenticate("not-a-token"); assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
