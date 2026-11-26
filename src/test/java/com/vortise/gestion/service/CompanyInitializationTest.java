package com.vortise.gestion.service;

import com.vortise.gestion.service.empresa.EmpresaConfiguracionService;
import com.vortise.gestion.service.equipo.EquipoAccessPolicy;
import com.vortise.gestion.service.equipo.UsuarioEmpresaService;
import com.vortise.gestion.service.pagos.SuscripcionConsultaService;
import com.vortise.gestion.application.dto.*;
import com.vortise.gestion.application.exception.*;
import com.vortise.gestion.model.*;
import com.vortise.gestion.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyInitializationTest {
    private final EmpresaRepository companies = mock(EmpresaRepository.class);
    private final UnidadMetricaEmpresaRepository units = mock(UnidadMetricaEmpresaRepository.class);
    private final CategoriaManoObraEmpresaRepository categories = mock(CategoriaManoObraEmpresaRepository.class);
    private Empresa company() {
        Empresa e = new Empresa("Test", null); ReflectionTestUtils.setField(e, "id", 1L);
        when(companies.findById(1L)).thenReturn(Optional.of(e));
        when(companies.bloquearPorId(1L)).thenReturn(Optional.of(e)); return e;
    }
    @Test void readsNeverWriteAndMissingCalendarIsExplicit() {
        company();
        var service = new EmpresaConfiguracionService(companies, mock(ResponsableEmpresaRepository.class), units,
            mock(CatalogoEmpresaRepository.class), categories);
        assertEquals(List.of(), service.getUnidades(1L));
        assertEquals(List.of(), service.getCategoriasManoObra(1L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.getCalendario(1L));
        verify(units, never()).save(any()); verify(categories, never()).save(any()); verify(companies, never()).save(any());
    }
    @Test void fillsMissingDefaultsOnceWithoutOverwritingCustomizationsOrRestoringDeletions() {
        Empresa company = company();
        UnidadMetricaEmpresa custom = new UnidadMetricaEmpresa(company, "ML", "Custom name");
        when(units.findByEmpresaId(1L)).thenReturn(List.of(custom));
        var useCase = new EmpresaConfiguracionService(companies, mock(ResponsableEmpresaRepository.class), units, mock(CatalogoEmpresaRepository.class), categories);
        useCase.inicializar(1L);
        assertTrue(company.isConfiguracionInicializada());
        assertEquals("Custom name", custom.getNombre());
        verify(units, times(4)).save(any()); verify(categories, times(4)).save(any()); assertNotNull(company.getCalendarioLaboral());
        clearInvocations(units, categories);
        // An intentional deletion after preparation must not be silently undone.
        when(units.findByEmpresaId(1L)).thenReturn(List.of());
        useCase.inicializar(1L);
        verifyNoInteractions(units, categories);
    }
    @Test void initializedCompanyWithoutCalendarGetsOnlyTheDefaultCalendar() {
        Empresa company = company();
        company.marcarConfiguracionInicializada();
        var useCase = new EmpresaConfiguracionService(companies, mock(ResponsableEmpresaRepository.class), units, mock(CatalogoEmpresaRepository.class), categories);
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.getCalendario(1L));

        useCase.inicializar(1L);

        assertNotNull(useCase.getCalendario(1L));
        verifyNoInteractions(units, categories);
        verify(companies).save(company);
    }
    @Test void editingCalendarKeepsItsIdAndUpdatesOnlyTheOwningCompany() {
        Empresa company = company();
        var service = new EmpresaConfiguracionService(companies, mock(ResponsableEmpresaRepository.class), units,
            mock(CatalogoEmpresaRepository.class), categories);

        var first = service.saveCalendario(1L, new CalendarioLaboralEmpresaDto(null, 7.5, 0., false, true, false, true));
        var edited = service.saveCalendario(1L, new CalendarioLaboralEmpresaDto(99L, 8., 4., true, false, true, false));

        assertNotNull(company.getCalendarioLaboral());
        assertNotNull(first.id());
        assertEquals(first.id(), edited.id());
        assertEquals(new CalendarioLaboralEmpresaDto(first.id(), 8., 4., true, false, true, false),
            service.getCalendario(1L));
    }

    @Test void administrativePasswordUpdateUsesTheSamePolicyAndMissingPasswordKeepsHash() {
        Empresa company = company();
        var users = mock(UsuarioEmpresaRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var plans = mock(SuscripcionConsultaService.class);
        UsuarioEmpresa user = new UsuarioEmpresa(company, "test@example.invalid", "Name", "existing-hash", RolEmpresa.CONSULTA);
        ReflectionTestUtils.setField(user, "id", 2L);
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        var service = new UsuarioEmpresaService(companies, users, encoder, plans, org.mockito.Mockito.mock(EquipoAccessPolicy.class), org.mockito.Mockito.mock(com.vortise.gestion.application.port.CurrentIdentity.class));
        assertThrows(SolicitudInvalidaException.class, () -> service.update(1L, 2L,
            new UsuarioEmpresaUpdateDto("Name", RolEmpresa.CONSULTA, true, "a")));
        verify(users, never()).save(any()); verifyNoInteractions(encoder);
        service.update(1L, 2L, new UsuarioEmpresaUpdateDto("Name", RolEmpresa.CONSULTA, true, null));
        assertEquals("existing-hash", user.getPasswordHash());
    }
}
