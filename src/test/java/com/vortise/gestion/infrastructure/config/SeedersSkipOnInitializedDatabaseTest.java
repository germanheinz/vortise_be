package com.vortise.gestion.infrastructure.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import com.vortise.gestion.application.port.StripeGateway;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.EntregaRepository;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import com.vortise.gestion.domain.repository.PlantaRepository;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RendimientoRepository;
import com.vortise.gestion.domain.repository.ResponsableEmpresaRepository;
import com.vortise.gestion.domain.repository.TareaRepository;
import com.vortise.gestion.domain.repository.UnidadMetricaEmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

class SeedersSkipOnInitializedDatabaseTest {
    @Test
    void allStartupSeedersSkipWhenDatabaseWasAlreadyInitialized() {
        SeedRunPolicy skipSeeders = () -> false;
        ProyectoRepository proyectos = mock(ProyectoRepository.class);
        PlantaRepository plantas = mock(PlantaRepository.class);
        EntregaRepository entregas = mock(EntregaRepository.class);
        TareaRepository tareas = mock(TareaRepository.class);
        RendimientoRepository rendimientos = mock(RendimientoRepository.class);
        IngresoRepository ingresos = mock(IngresoRepository.class);
        GastoFijoRepository gastos = mock(GastoFijoRepository.class);
        PresupuestoLineaRepository presupuestos = mock(PresupuestoLineaRepository.class);
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        ResponsableEmpresaRepository responsables = mock(ResponsableEmpresaRepository.class);
        UnidadMetricaEmpresaRepository unidades = mock(UnidadMetricaEmpresaRepository.class);
        CatalogoEmpresaRepository catalogos = mock(CatalogoEmpresaRepository.class);
        CategoriaManoObraEmpresaRepository categorias = mock(CategoriaManoObraEmpresaRepository.class);
        CalendarioLaboralEmpresaRepository calendarios = mock(CalendarioLaboralEmpresaRepository.class);
        new DataSeeder(proyectos, plantas, entregas, tareas, rendimientos, ingresos, gastos, presupuestos,
            empresas, responsables, unidades, catalogos, categorias, calendarios, skipSeeders).run();

        EmpresaRepository demoEmpresas = mock(EmpresaRepository.class);
        UsuarioEmpresaRepository demoUsuarios = mock(UsuarioEmpresaRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        new UsuarioDemoSeeder(demoEmpresas, demoUsuarios, encoder, skipSeeders).run();

        EmpresaRepository platformEmpresas = mock(EmpresaRepository.class);
        UsuarioEmpresaRepository platformUsuarios = mock(UsuarioEmpresaRepository.class);
        PasswordEncoder platformEncoder = mock(PasswordEncoder.class);
        when(platformUsuarios.findByEmail("root@example.invalid")).thenReturn(Optional.empty());
        when(platformEmpresas.findByIdentificacionFiscal(SuperAdminSeeder.PLATFORM_TAX_ID)).thenReturn(Optional.empty());
        when(platformEmpresas.findByNombreIgnoreCase(SuperAdminSeeder.PLATFORM_NAME)).thenReturn(Optional.empty());
        when(platformEmpresas.save(any(com.vortise.gestion.domain.model.Empresa.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(platformEncoder.encode("long-enough-password")).thenReturn("password-hash");
        new SuperAdminSeeder(platformEmpresas, platformUsuarios, platformEncoder,
            "root@example.invalid", "long-enough-password", skipSeeders).run();
        verify(platformUsuarios).save(argThat(user -> user.isSuperAdmin()
            && "root@example.invalid".equals(user.getEmail())
            && "password-hash".equals(user.getPasswordHash())));

        PlanSuscripcionRepository planes = mock(PlanSuscripcionRepository.class);
        StripeGateway stripe = mock(StripeGateway.class);
        new PlanSuscripcionSeeder(planes, stripe, "", "", skipSeeders).run();

        verifyNoInteractions(proyectos, plantas, entregas, tareas, rendimientos, ingresos, gastos,
            presupuestos, empresas, responsables, unidades, catalogos, categorias, calendarios,
            demoEmpresas, demoUsuarios, encoder, planes, stripe);
    }
}