package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.UsuarioEmpresaUpdateDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class UsuarioObrasAsignadasTest {
    private final EmpresaRepository companies = mock(EmpresaRepository.class);
    private final UsuarioEmpresaRepository users = mock(UsuarioEmpresaRepository.class);
    private final ProyectoRepository projects = mock(ProyectoRepository.class);
    private final CurrentIdentity identity = mock(CurrentIdentity.class);
    private final UsuarioEmpresaUseCase useCase = new UsuarioEmpresaUseCase(companies, users, mock(PasswordEncoder.class),
        mock(SuscripcionConsultaService.class), mock(EquipoAccessPolicy.class), identity, projects);
    private Empresa company;
    private UsuarioEmpresa jefe;

    @BeforeEach
    void setup() {
        company = new Empresa("Own", null);
        ReflectionTestUtils.setField(company, "id", 1L);
        jefe = new UsuarioEmpresa(company, "jefe@example.invalid", "Jefe", "hash", RolEmpresa.JEFE_DE_OBRA);
        ReflectionTestUtils.setField(jefe, "id", 2L);
        when(companies.bloquearPorId(1L)).thenReturn(Optional.of(company));
        when(users.findById(2L)).thenReturn(Optional.of(jefe));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void site(long id, Empresa owner) {
        var project = new Proyecto();
        project.setId(id);
        project.setEmpresaRelacionada(owner);
        when(projects.findById(id)).thenReturn(Optional.of(project));
    }

    private UsuarioEmpresaUpdateDto update(RolEmpresa role, List<Long> sites) {
        return new UsuarioEmpresaUpdateDto("Jefe", role, true, null, sites);
    }

    @Test
    void assignsSitesOfTheSameCompanyAndRevokesCurrentSessions() {
        site(7L, company);
        long before = jefe.getSessionVersion();

        var result = useCase.update(1L, 2L, update(RolEmpresa.JEFE_DE_OBRA, List.of(7L)));

        assertEquals(Set.of(7L), jefe.getObrasAsignadas());
        assertEquals(List.of(7L), result.obrasAsignadas());
        assertNotEquals(before, jefe.getSessionVersion());
    }

    @Test
    void rejectsASiteOfAnotherCompany() {
        var other = new Empresa("Other", null);
        ReflectionTestUtils.setField(other, "id", 9L);
        site(8L, other);

        assertThrows(SolicitudInvalidaException.class, () -> useCase.update(1L, 2L, update(RolEmpresa.JEFE_DE_OBRA, List.of(8L))));
        assertTrue(jefe.getObrasAsignadas().isEmpty());
        verify(users, never()).save(any());
    }

    @Test
    void rejectsAnUnknownSite() {
        when(projects.findById(404L)).thenReturn(Optional.empty());

        assertThrows(SolicitudInvalidaException.class, () -> useCase.update(1L, 2L, update(RolEmpresa.JEFE_DE_OBRA, List.of(404L))));
    }

    @Test
    void onlyAJefeCanHaveAssignedSites() {
        site(7L, company);

        assertThrows(SolicitudInvalidaException.class, () -> useCase.update(1L, 2L, update(RolEmpresa.CONSULTA, List.of(7L))));
    }

    @Test
    void changingTheRoleAwayFromJefeClearsItsSites() {
        site(7L, company);
        jefe.asignarObras(Set.of(7L));

        useCase.update(1L, 2L, update(RolEmpresa.CONSULTA, null));

        assertTrue(jefe.getObrasAsignadas().isEmpty());
    }

    @Test
    void omittingSitesLeavesThemAndTheSessionUntouched() {
        jefe.asignarObras(Set.of(7L));
        long before = jefe.getSessionVersion();

        useCase.update(1L, 2L, update(RolEmpresa.JEFE_DE_OBRA, null));

        assertEquals(Set.of(7L), jefe.getObrasAsignadas());
        assertEquals(before, jefe.getSessionVersion());
    }

    @Test
    void anAdministratorCannotManageAPlatformSuperAdmin() {
        jefe.setSuperAdmin(true);
        when(identity.isSuperAdmin()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> useCase.update(1L, 2L, update(RolEmpresa.JEFE_DE_OBRA, null)));
        verify(users, never()).save(any());
    }
}
