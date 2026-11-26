package com.vortise.gestion.service;

import com.vortise.gestion.application.dto.CalendarioObraDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoUpdateDto;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.CalendarioLaboralEmpresa;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Spec 016: calendario laboral por obra. */
class CalendarioLaboralObraTest {
    private static final Long EMPRESA_ID = 7L;
    private ProyectoRepository proyectos;
    private Empresa empresa;
    private ProyectoAltaService create;

    @BeforeEach
    void setUp() {
        proyectos = mock(ProyectoRepository.class);
        empresa = new Empresa("A", null);
        ReflectionTestUtils.setField(empresa, "id", EMPRESA_ID);
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        when(empresas.bloquearPorId(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        var cambios = mock(ConfiguracionEmpresaObraService.class);
        when(cambios.execute(any(), any(), any())).thenReturn(List.of());
        when(proyectos.save(any(Proyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        create = new ProyectoAltaService(proyectos, empresas, mock(CatalogoEmpresaRepository.class), mock(CurrentIdentity.class), cambios);
    }

    @Test
    void newWorkStartsFromTheCompanyCalendarWhenTheRequestOmitsIt() {
        var company = empresa.inicializarCalendarioLaboral();
        company.setHorasSabado(0.0);
        company.setTrabajaDomingo(true);
        company.setPagaDobleFeriado(true);

        var saved = create.execute(request(null));

        assertEquals(new CalendarioObraDto(false, true, true, false), saved.calendarioLaboral());
    }

    @Test
    void theRequestedCalendarOverridesTheCompanyOne() {
        var company = empresa.inicializarCalendarioLaboral();
        company.setTrabajaDomingo(true);
        var requested = new CalendarioObraDto(false, false, false, true);

        var saved = create.execute(request(requested));

        assertEquals(requested, saved.calendarioLaboral());
    }

    @Test
    void updateChangesTheCalendarOnlyWhenItIsSent() {
        var projects = mock(ProyectoRepository.class);
        var identity = mock(CurrentIdentity.class);
        Proyecto project = new Proyecto();
        project.setId(7L);
        project.setEmpresaRelacionada(empresa);
        project.setCalendarioLaboral(true, true, false, false);
        when(projects.bloquearPorId(7L)).thenReturn(Optional.of(project));
        when(projects.save(project)).thenReturn(project);
        var update = new ProyectoEdicionService(projects, mock(EmpresaRepository.class), mock(CatalogoEmpresaRepository.class),
            mock(ObraTareaRepository.class), mock(RestriccionObraRepository.class), identity, mock(RegistroHorasRepository.class));

        assertEquals(new CalendarioObraDto(true, true, false, false), update.execute(7L,
            new ProyectoUpdateDto("Obra", null, null, "Dir", "P1", null, null, null, null,
                null, null, null, null, null, null, null, null)).calendarioLaboral());

        var requested = new CalendarioObraDto(false, false, true, true);
        var saved = update.execute(7L, new ProyectoUpdateDto("Obra", null, null, "Dir", "P1", null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, requested));
        assertEquals(requested, saved.calendarioLaboral());
    }

    private ProyectoCreateDto request(CalendarioObraDto calendar) {
        return new ProyectoCreateDto("Obra", null, null, "Dirección", "OB-1", null, null, null, null, null, EMPRESA_ID, null,
            null, null, List.of(), List.of(), List.of(), null, null, null, null, null, null, null, calendar);
    }
}
