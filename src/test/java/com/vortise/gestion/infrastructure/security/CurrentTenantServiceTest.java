package com.vortise.gestion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import io.jsonwebtoken.Jwts;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentTenantServiceTest {
    private static final long USER_ID = 7L;
    private static final long OWN_COMPANY = 1L;
    private static final long FOREIGN_COMPANY = 2L;

    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final CurrentTenantService service = new CurrentTenantService(empresas);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anyRoleAccessesOwnCompanyWithoutLookup() {
        authenticate(OWN_COMPANY, "ROLE_CONSULTA");
        assertDoesNotThrow(() -> service.requireEmpresa(OWN_COMPANY));
        verifyNoInteractions(empresas);
    }

    @Test
    void nonAdminIsDeniedForeignCompanyEvenIfTheyCreatedIt() {
        authenticate(OWN_COMPANY, "ROLE_JEFE_DE_OBRA");
        when(empresas.findById(FOREIGN_COMPANY)).thenReturn(Optional.of(company(USER_ID)));
        assertThrows(AccessDeniedException.class, () -> service.requireEmpresa(FOREIGN_COMPANY));
    }

    @Test
    void adminAccessesCompanyTheyCreated() {
        authenticate(OWN_COMPANY, "ROLE_ADMINISTRADOR");
        when(empresas.findById(FOREIGN_COMPANY)).thenReturn(Optional.of(company(USER_ID)));
        assertDoesNotThrow(() -> service.requireEmpresa(FOREIGN_COMPANY));
    }

    @Test
    void adminIsDeniedCompanyCreatedByAnotherUser() {
        authenticate(OWN_COMPANY, "ROLE_ADMINISTRADOR");
        when(empresas.findById(FOREIGN_COMPANY)).thenReturn(Optional.of(company(99L)));
        assertThrows(AccessDeniedException.class, () -> service.requireEmpresa(FOREIGN_COMPANY));
    }

    @Test
    void adminIsDeniedLegacyCompanyWithoutCreator() {
        authenticate(OWN_COMPANY, "ROLE_ADMINISTRADOR");
        when(empresas.findById(FOREIGN_COMPANY)).thenReturn(Optional.of(company(null)));
        assertThrows(AccessDeniedException.class, () -> service.requireEmpresa(FOREIGN_COMPANY));
    }

    @Test
    void adminIsDeniedMissingCompany() {
        authenticate(OWN_COMPANY, "ROLE_ADMINISTRADOR");
        when(empresas.findById(FOREIGN_COMPANY)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.requireEmpresa(FOREIGN_COMPANY));
    }

    @Test
    void superAdminAccessesAnyCompanyWithoutLookup() {
        authenticate(OWN_COMPANY, "ROLE_ADMINISTRADOR", "ROLE_SUPER_ADMIN");
        assertDoesNotThrow(() -> service.requireEmpresa(FOREIGN_COMPANY));
        verifyNoInteractions(empresas);
    }

    @Test
    void userWithoutCompanyClaimKeepsConflictBehavior() {
        authenticate(null, "ROLE_ADMINISTRADOR");
        assertThrows(IllegalStateException.class, () -> service.requireEmpresa(FOREIGN_COMPANY));
    }

    @Test
    void exposesUserIdFromToken() {
        authenticate(OWN_COMPANY, "ROLE_CONSULTA");
        assertEquals(USER_ID, service.usuarioId());
    }

    private static Empresa company(Long creatorId) {
        Empresa empresa = new Empresa("Foreign", null);
        empresa.setCreadoPorUsuarioId(creatorId);
        return empresa;
    }

    private static void authenticate(Long companyId, String... authorities) {
        var claimsBuilder = Jwts.claims().add("userId", USER_ID);
        if (companyId != null) claimsBuilder.add("empresaId", companyId);
        var authentication = new UsernamePasswordAuthenticationToken("user@example.invalid", null,
            Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        authentication.setDetails(claimsBuilder.build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
