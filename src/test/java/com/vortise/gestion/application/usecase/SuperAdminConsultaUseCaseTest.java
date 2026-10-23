package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.PagoSuscripcionRepository;
import com.vortise.gestion.domain.repository.SuscripcionRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SuperAdminConsultaUseCaseTest {
    @Mock private EmpresaRepository empresas;
    @Mock private UsuarioEmpresaRepository usuarios;
    @Mock private SuscripcionRepository suscripciones;
    @Mock private PagoSuscripcionRepository pagos;
    private SuperAdminConsultaUseCase consulta;

    @BeforeEach
    void setUp() {
        consulta = new SuperAdminConsultaUseCase(empresas, usuarios, suscripciones, pagos);
    }

    @Test
    @SuppressWarnings("null")
    void userListMapsOnlyFilteredPageAndKeepsCompanyContext() {
        Empresa empresa = new Empresa("Constructora", null);
        UsuarioEmpresa usuario = new UsuarioEmpresa(empresa, "user@example.invalid", "Usuario", "hash", RolEmpresa.CONSULTA);
        List<UsuarioEmpresa> content = new ArrayList<>();
        content.add(usuario);
        Empresa empresaInterna = org.mockito.Mockito.mock(Empresa.class);
        when(empresaInterna.getId()).thenReturn(99L);
        when(empresas.findByIdentificacionFiscal("PLATFORM-0001")).thenReturn(Optional.of(empresaInterna));
        when(usuarios.buscarSuperAdmin(eq(99L), eq(42L), eq(true), eq("user"), any()))
            .thenReturn(new PageImpl<>(content, PageRequest.of(1, 25), 1));

        var result = consulta.usuarios(1, 25, " user ", 42L, true);

        assertEquals(1, result.content().size());
        assertEquals("Constructora", result.content().get(0).empresaNombre());
        assertEquals("user@example.invalid", result.content().get(0).email());
        assertEquals(1, result.page());
        verify(usuarios).buscarSuperAdmin(eq(99L), eq(42L), eq(true), eq("user"), any());
    }

    @Test
    @SuppressWarnings("null")
    void userListUsesEmptySearchTextWhenQueryIsMissing() {
        when(empresas.findByIdentificacionFiscal("PLATFORM-0001")).thenReturn(Optional.empty());
        when(usuarios.buscarSuperAdmin(eq(null), eq(null), eq(null), eq(""), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        consulta.usuarios(0, 20, null, null, null);

        verify(usuarios).buscarSuperAdmin(eq(null), eq(null), eq(null), eq(""), any());
    }

    @Test
    @SuppressWarnings("null")
    void companyListUsesEmptySearchTextWhenQueryIsBlank() {
        when(empresas.findByIdentificacionFiscal("PLATFORM-0001")).thenReturn(Optional.empty());
        when(empresas.buscarSuperAdmin(eq(null), eq(""), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        consulta.empresas(0, 20, "   ");

        verify(empresas).buscarSuperAdmin(eq(null), eq(""), any());
    }

    @Test
    void dashboardKeepsPaymentTotalsSeparatedByCurrency() {
        when(empresas.findByIdentificacionFiscal("PLATFORM-0001")).thenReturn(Optional.empty());
        when(empresas.count()).thenReturn(2L);
        when(usuarios.contarActivosSuperAdmin(null)).thenReturn(3L);
        when(usuarios.contarInactivosSuperAdmin(null)).thenReturn(1L);
        when(suscripciones.ultimaActualizacionSuperAdmin(null)).thenReturn(null);
        when(suscripciones.contarEstadosSuperAdmin(null)).thenReturn(List.<Object[]>of(new Object[] {"active", 2L}));
        when(suscripciones.contarEstadoSuperAdmin(null, "past_due")).thenReturn(0L);
        when(suscripciones.contarInactivasSuperAdmin(null)).thenReturn(0L);
        when(suscripciones.contarPruebasPorVencer(eq(null), any(), any())).thenReturn(0L);
        when(pagos.ultimaActualizacionSuperAdmin(null)).thenReturn(null);
        when(pagos.resumirRecientesSuperAdmin(eq(null), any())).thenReturn(List.<Object[]>of(
            new Object[] {"PAGADO", "ARS", 2L, 2500L}, new Object[] {"PAGADO", "USD", 1L, 1000L}));
        when(pagos.contarFallidosSuperAdmin(null)).thenReturn(1L);

        var result = consulta.dashboard();

        assertEquals(2, result.empresas());
        assertEquals(3, result.usuariosActivos());
        assertEquals(2, result.pagosRecientesPorEstadoYMoneda().size());
        assertEquals("ARS", result.pagosRecientesPorEstadoYMoneda().get(0).moneda());
        assertEquals("USD", result.pagosRecientesPorEstadoYMoneda().get(1).moneda());
    }
}
