package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.Rendimiento;
import com.vortise.gestion.domain.repository.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// Pure mocks: no Spring context, JDBC or persistent fixtures.
@ExtendWith(MockitoExtension.class)
class DataSeederTest {
    @Mock ProyectoRepository proyectos;
    @Mock PlantaRepository plantas;
    @Mock EntregaRepository entregas;
    @Mock TareaRepository tareas;
    @Mock RendimientoRepository rendimientos;
    @Mock IngresoRepository ingresos;
    @Mock GastoFijoRepository gastos;
    @Mock PresupuestoLineaRepository presupuestos;
    @Mock EmpresaRepository empresas;
    @Mock ResponsableEmpresaRepository responsables;
    @Mock UnidadMetricaEmpresaRepository unidades;
    @Mock CatalogoEmpresaRepository catalogos;
    @Mock CategoriaManoObraEmpresaRepository categorias;
    @Mock CalendarioLaboralEmpresaRepository calendarios;
    @Mock SeedRunPolicy seedRunPolicy;
    @InjectMocks DataSeeder seeder;

    @BeforeEach
    void allowSeedRunsByDefault() {
        lenient().when(seedRunPolicy.shouldRunSeeders()).thenReturn(true);
    }

    private void existingProject(Empresa empresa) {
        ReflectionTestUtils.setField(Objects.requireNonNull(empresa), "id", 1L);
        Proyecto project = new Proyecto();
        project.setNombre("PROYECTO DEMO CRONOGRAMA");
        project.setEmpresaRelacionada(empresa);
        when(proyectos.findAll()).thenReturn(List.of(project));
        when(rendimientos.findAll()).thenReturn(List.of(mock(Rendimiento.class)));
    }

    @Test void renamedCompanyIsReusedWithoutOverwritingItsFields() {
        Empresa empresa = new Empresa("Renamed", "DEMO-0001");
        empresa.setLimiteUsuarios(12);
        existingProject(empresa);
        when(empresas.findByIdentificacionFiscal("DEMO-0001")).thenReturn(Optional.of(empresa));
        seeder.run();
        seeder.run();
        verify(empresas, never()).save(any());
        verify(empresas, never()).findByNombreIgnoreCase(any());
        verify(proyectos, never()).save(any());
        assertEquals("Renamed", empresa.getNombre());
        assertEquals("DEMO-0001", empresa.getIdentificacionFiscal());
        assertEquals(12, empresa.getLimiteUsuarios());
    }

    @Test void createsCompanyWithDefaultLimitOnlyWhenBothIdentifiersAreFree() {
        Empresa empresa = new Empresa("Constructora Norte", "DEMO-0001");
        existingProject(empresa);
        when(empresas.save(any())).thenAnswer(invocation -> {
            Empresa created = invocation.getArgument(0);
            assertEquals("DEMO-0001", created.getIdentificacionFiscal());
            assertEquals("Constructora Norte", created.getNombre());
            assertEquals(25, created.getLimiteUsuarios());
            return empresa;
        });
        seeder.run();
        verify(empresas).findByIdentificacionFiscal("DEMO-0001");
        verify(empresas).findByNombreIgnoreCase("Constructora Norte");
        verify(empresas).save(any());
    }

    @Test void conflictingNameFailsBeforeAnySeedWrites() {
        when(empresas.findByNombreIgnoreCase("Constructora Norte"))
            .thenReturn(Optional.of(new Empresa("Constructora Norte", "OTHER")));
        assertThrows(IllegalStateException.class, seeder::run);
        verify(empresas, never()).save(any());
        verifyNoInteractions(proyectos, plantas, entregas, tareas, rendimientos, ingresos,
            gastos, presupuestos, responsables, unidades, catalogos, categorias, calendarios);
    }

    @Test void explicitDemoResetStillRunsWhenStartupSeedingIsDisabled() {
        lenient().when(seedRunPolicy.shouldRunSeeders()).thenReturn(false);
        Empresa empresa = new Empresa("Constructora Norte", "DEMO-0001");
        ReflectionTestUtils.setField(empresa, "id", 1L);
        when(empresas.findByIdentificacionFiscal("DEMO-0001")).thenReturn(Optional.of(empresa));
        when(responsables.findByEmpresaId(1L)).thenReturn(List.of());
        when(unidades.findByEmpresaId(1L)).thenReturn(List.of());
        when(catalogos.findByEmpresaIdAndTipo(1L, "SUBCONTRATISTA")).thenReturn(List.of());
        when(catalogos.findByEmpresaIdAndTipo(1L, "TIPO_RESTRICCION")).thenReturn(List.of());
        when(catalogos.findByEmpresaIdAndTipo(1L, "CAUSA_INCUMPLIMIENTO")).thenReturn(List.of());
        when(categorias.findByEmpresaId(1L)).thenReturn(List.of());
        when(calendarios.findByEmpresaId(1L)).thenReturn(Optional.empty());
        when(calendarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(proyectos.findAll()).thenReturn(List.of());
        when(proyectos.save(any(Proyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gastos.findAll()).thenReturn(List.of());
        when(rendimientos.findAll()).thenReturn(List.of());

        seeder.resetDemoData();

        verify(seedRunPolicy, never()).shouldRunSeeders();
        verify(proyectos, atLeastOnce()).save(any(Proyecto.class));
    }
}
