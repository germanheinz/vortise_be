package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.*;
import com.vortise.gestion.application.exception.*;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
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
    private final CalendarioLaboralEmpresaRepository calendars = mock(CalendarioLaboralEmpresaRepository.class);
    private Empresa company() {
        Empresa e = new Empresa("Test", null); ReflectionTestUtils.setField(e, "id", 1L);
        when(companies.findById(1L)).thenReturn(Optional.of(e));
        when(companies.bloquearPorId(1L)).thenReturn(Optional.of(e)); return e;
    }
    @Test void readsNeverWriteAndMissingCalendarIsExplicit() {
        company();
        var service = new EmpresaConfiguracionUseCase(companies, mock(ResponsableEmpresaRepository.class), units,
            mock(CatalogoEmpresaRepository.class), calendars, categories);
        assertEquals(List.of(), service.getUnidades(1L));
        assertEquals(List.of(), service.getCategoriasManoObra(1L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.getCalendario(1L));
        verify(units, never()).save(any()); verify(categories, never()).save(any()); verify(calendars, never()).save(any());
    }
    @Test void fillsMissingDefaultsOnceWithoutOverwritingCustomizationsOrRestoringDeletions() {
        Empresa company = company();
        UnidadMetricaEmpresa custom = new UnidadMetricaEmpresa(company, "ML", "Custom name");
        when(units.findByEmpresaId(1L)).thenReturn(List.of(custom));
        var useCase = new InicializarEmpresaUseCase(companies, units, categories, calendars);
        useCase.execute(1L);
        assertTrue(company.isConfiguracionInicializada());
        assertEquals("Custom name", custom.getNombre());
        verify(units, times(4)).save(any()); verify(categories, times(4)).save(any()); verify(calendars).save(any());
        clearInvocations(units, categories, calendars);
        // An intentional deletion after preparation must not be silently undone.
        when(units.findByEmpresaId(1L)).thenReturn(List.of());
        useCase.execute(1L);
        verifyNoInteractions(units, categories, calendars);
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
        var service = new UsuarioEmpresaUseCase(companies, users, encoder, plans);
        assertThrows(SolicitudInvalidaException.class, () -> service.update(1L, 2L,
            new UsuarioEmpresaUpdateDto("Name", RolEmpresa.CONSULTA, true, "a")));
        verify(users, never()).save(any()); verifyNoInteractions(encoder);
        service.update(1L, 2L, new UsuarioEmpresaUpdateDto("Name", RolEmpresa.CONSULTA, true, null));
        assertEquals("existing-hash", user.getPasswordHash());
    }
}
