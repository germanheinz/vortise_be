package com.vortise.gestion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.ObraTareaRepository;
import com.vortise.gestion.repository.ProyectoRepository;
import com.vortise.gestion.repository.RegistroHorasRepository;
import com.vortise.gestion.repository.RestriccionObraRepository;
import com.vortise.gestion.model.RestriccionObra;
import io.jsonwebtoken.Jwts;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class TenantAccessInterceptorTest {
    private static final long COMPANY = 1L;
    private static final long ASSIGNED_SITE = 7L;
    private static final long OTHER_SITE = 8L;

    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final RestriccionObraRepository restricciones = mock(RestriccionObraRepository.class);
    private final CurrentTenantService tenant = new CurrentTenantService(mock(EmpresaRepository.class),
        mock(com.vortise.gestion.repository.UsuarioEmpresaRepository.class),
        mock(com.vortise.gestion.service.equipo.EquipoAccessPolicy.class));
    private final TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenant, proyectos, restricciones,
        mock(RegistroHorasRepository.class), mock(ObraTareaRepository.class));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Proyecto site(long id) {
        var company = new Empresa("Own", null);
        ReflectionTestUtils.setField(company, "id", COMPANY);
        var project = new Proyecto();
        project.setId(id);
        project.setEmpresaRelacionada(company);
        when(proyectos.findById(id)).thenReturn(Optional.of(project));
        return project;
    }

    private static void authenticate(List<Long> sites, String role) {
        var claims = Jwts.claims().add("userId", 5L).add("empresaId", COMPANY).add("obras", sites).build();
        var authentication = new UsernamePasswordAuthenticationToken("user@example.invalid", null,
            Arrays.stream(new String[] {role}).map(SimpleGrantedAuthority::new).toList());
        authentication.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean handle(String method, String path) {
        return interceptor.preHandle(new MockHttpServletRequest(method, path) {{ setRequestURI(path); }},
            new MockHttpServletResponse(), new Object());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void jefeCannotWriteOnASiteThatIsNotAssigned(String method) {
        site(OTHER_SITE);
        authenticate(List.of(ASSIGNED_SITE), "ROLE_JEFE_DE_OBRA");

        assertThrows(ObraNoAsignadaException.class, () -> handle(method, "/api/proyectos/" + OTHER_SITE + "/restricciones"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void jefeCanWriteOnAnAssignedSite(String method) {
        site(ASSIGNED_SITE);
        authenticate(List.of(ASSIGNED_SITE), "ROLE_JEFE_DE_OBRA");

        assertDoesNotThrow(() -> handle(method, "/api/proyectos/" + ASSIGNED_SITE + "/restricciones"));
    }

    @Test
    void jefeCanReadAnySiteOfItsCompany() {
        site(OTHER_SITE);
        authenticate(List.of(ASSIGNED_SITE), "ROLE_JEFE_DE_OBRA");

        assertDoesNotThrow(() -> handle("GET", "/api/proyectos/" + OTHER_SITE + "/restricciones"));
    }

    @Test
    void jefeWithoutAssignedSitesCannotWriteAnywhere() {
        site(ASSIGNED_SITE);
        authenticate(List.of(), "ROLE_JEFE_DE_OBRA");

        assertThrows(ObraNoAsignadaException.class, () -> handle("POST", "/api/proyectos/" + ASSIGNED_SITE + "/restricciones"));
    }

    @Test
    void otherRolesAreNotRestrictedBySites() {
        site(OTHER_SITE);
        authenticate(List.of(), "ROLE_ADMINISTRADOR");

        assertDoesNotThrow(() -> handle("POST", "/api/proyectos/" + OTHER_SITE + "/restricciones"));
    }

    @Test
    void childResourceSiteIsCheckedThroughItsOwner() {
        var project = site(OTHER_SITE);
        var restriction = mock(RestriccionObra.class);
        when(restriction.getProyecto()).thenReturn(project);
        when(restricciones.findById(any())).thenReturn(Optional.of(restriction));
        authenticate(List.of(ASSIGNED_SITE), "ROLE_JEFE_DE_OBRA");

        assertThrows(ObraNoAsignadaException.class, () -> handle("PATCH", "/api/restricciones/99"));
        assertDoesNotThrow(() -> handle("GET", "/api/restricciones/99"));
    }
}
