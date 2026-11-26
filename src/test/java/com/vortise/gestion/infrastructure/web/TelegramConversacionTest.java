package com.vortise.gestion.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.vortise.gestion.application.dto.EmpresaDashboardObrasDto;
import com.vortise.gestion.application.service.RestriccionObraConsultaService;
import com.vortise.gestion.application.service.EmpresaDashboardObrasService;
import com.vortise.gestion.application.service.ReporteObraService;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelegramConversacionTest {
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final CurrentTenantService identidad = mock(CurrentTenantService.class);
    private final ReporteObraService reportes = mock(ReporteObraService.class);
    private final RestriccionObraConsultaService restricciones = mock(RestriccionObraConsultaService.class);
    private final EmpresaDashboardObrasService dashboard = mock(EmpresaDashboardObrasService.class);
    private final TelegramConversacion flujo = new TelegramConversacion(proyectos, empresas, identidad, reportes, restricciones, dashboard, "https://app.vortise.test/");
    @SuppressWarnings("unchecked")
    private final UnaryOperator<String> ejecutar = mock(UnaryOperator.class);

    @BeforeEach
    void obras() {
        Empresa empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(2L);
        var obra = new Proyecto();
        obra.setId(5L);
        obra.setNombre("Obra Norte");
        obra.setEmpresaRelacionada(empresa);
        when(identidad.empresaId()).thenReturn(2L);
        when(identidad.usuarioId()).thenReturn(8L);
        when(identidad.canAccessEmpresa(2L)).thenReturn(true);
        when(proyectos.findByEmpresaRelacionadaId(2L)).thenReturn(List.of(obra));
        when(proyectos.findById(5L)).thenReturn(Optional.of(obra));
    }

    @Test
    void conduceElAltaYUnEventoRepetidoNoLaEjecutaDosVeces() {
        var bienvenida = enviar(1, "Hola");
        assertThat(bienvenida.mensaje()).contains(
            "¡Hola, Ana! Soy VortiseBot. Me gustaría ayudarte.",
            "1. Consultar restricciones", "2. Crear restricción en obra", "3. Consultar estado de obra");
        assertThat(bienvenida.opciones()).containsExactly(
            "Consultar restricciones", "Crear restricción en obra", "Consultar estado de obra");
        assertThat(enviar(2, "op:1:1").opciones()).containsExactly("Obra Norte · #5", "Volver al menú");
        assertThat(enviar(3, "op:2:0").mensaje()).contains("Qué restricción");
        assertThat(enviar(4, "Aprobar plano eléctrico").mensaje()).contains("fecha límite");
        assertThat(enviar(5, "31/02/2099").mensaje()).contains("no es válida");

        String comando = "/nueva_restriccion 5 2099-11-27 Aprobar plano eléctrico";
        when(ejecutar.apply(comando)).thenReturn("Restricción #9 guardada");
        assertThat(enviar(6, "27/11/2099").mensaje()).contains("#9");
        assertThat(enviar(6, "27/11/2099").mensaje()).contains("#9");
        verify(ejecutar, times(1)).apply(comando);
    }

    @Test
    void vuelveAValidarLaObraElegidaAntesDeConsultar() {
        assertThat(enviar(1, "restricciones").mensaje()).contains("Obras disponibles", "1. Obra Norte · #5");
        when(identidad.canAccessEmpresa(2L)).thenReturn(false);

        assertThat(enviar(2, "Obra Norte · #5").mensaje()).contains("ya no está disponible");
        verifyNoInteractions(ejecutar);
    }

    @Test
    void permiteElegirAccionYObraPorNumeroCuandoElTecladoNoSeMuestra() {
        enviar(1, "Hola");
        assertThat(enviar(2, "1").mensaje()).contains("Obras disponibles", "1. Obra Norte · #5");
        when(ejecutar.apply("/restricciones 5")).thenReturn("Obra Norte · 2 restricciones");
        assertThat(enviar(3, "1").mensaje()).contains("Obra Norte · 2 restricciones");
        verify(ejecutar).apply("/restricciones 5");
    }

    @Test
    void conservaElPasoCuandoRecibeUnaOpcionInvalidaOUnAdjunto() {
        enviar(1, "Hola");
        assertThat(enviar(2, "9").mensaje()).contains("Esa opción no es válida", "Consultar restricciones");
        enviar(3, "2");
        assertThat(enviar(4, "9").mensaje()).contains("Esa obra no está en la lista", "Obra Norte");
        enviar(5, "1");
        assertThat(enviar(6, "/mensaje_no_admitido").mensaje()).contains("descripción de la restricción en texto");
        assertThat(enviar(7, "Revisar planos").mensaje()).contains("fecha límite");
        assertThat(enviar(8, "mañana").mensaje()).contains("La fecha no es válida");
        verifyNoInteractions(ejecutar);
    }

    @Test
    void elEstadoMuestraElDesvioEnVivoConElFormatoDeLasPantallasYEnlacesDeVortise() {
        when(restricciones.execute(eq(5L), any(), any(), any())).thenReturn(new RestriccionObraConsultaService.Respuesta(
            5L, "Obra Norte", null, 3, 3, 1, 0, "/x", List.of()));
        when(dashboard.obra(5L)).thenReturn(new EmpresaDashboardObrasDto.Obra(5L, "Obra Norte", null, -23346.4, 152198.0, -1.54, 3.04, 1500000.0, 5, 3, 3));
        when(reportes.listar(5L)).thenReturn(List.of());
        enviar(1, "estado");
        var respuesta = enviar(2, "Obra Norte · #5");

        assertThat(respuesta.mensaje()).contains("Obra Norte", "En vivo · mano de obra propia",
            "Desvío actual: −$ 23.346 (−1,5 %)", "Desvío proyectado: +$ 152.198 (+3,0 %)",
            "Actividades con datos: 3 de 5", "Restricciones: 3 abiertas · 1 vencidas", "Todavía no hay un reporte de obra emitido");
        assertThat(respuesta.mensaje()).doesNotContain("ARS");
        assertThat(respuesta.enlaces()).extracting(TelegramController.Enlace::url).containsExactly(
            "https://app.vortise.test/proyectos/5", "https://app.vortise.test/proyectos/5/restricciones");
    }

    @Test
    void sinDatosNoInventaCerosYSinHttpsNoEnviaEnlaces() {
        assertThat(TelegramConversacion.desvio(null, null)).isEqualTo("Sin datos");
        assertThat(TelegramConversacion.desvio(0.0, 0.0)).isEqualTo("$ 0 (0,0 %)");
        assertThat(TelegramConversacion.desvio(-24160.0, null)).isEqualTo("−$ 24.160");

        var local = new TelegramConversacion(proyectos, empresas, identidad, reportes, restricciones, dashboard, "http://localhost:3000");
        when(restricciones.execute(eq(5L), any(), any(), any())).thenReturn(new RestriccionObraConsultaService.Respuesta(
            5L, "Obra Norte", null, 0, 0, 0, 0, "/x", List.of()));
        when(dashboard.obra(5L)).thenReturn(new EmpresaDashboardObrasDto.Obra(5L, "Obra Norte", null, null, null, null, null, null, 0, 0, 0));
        when(reportes.listar(5L)).thenReturn(List.of());
        local.responder(10L, 8L, "Ana", 1, "estado", ejecutar);
        var respuesta = local.responder(10L, 8L, "Ana", 2, "Obra Norte · #5", ejecutar);
        assertThat(respuesta.enlaces()).isEmpty();
        assertThat(respuesta.mensaje()).contains("Desvío actual: Sin datos").doesNotContain("Actividades con datos");
    }

    @Test
    void ayudaExplicaLasOpcionesSinPerderElMenu() {
        var respuesta = enviar(1, "/ayuda");
        assertThat(respuesta.mensaje()).contains("/restricciones ID_OBRA", "DD/MM/AAAA");
        assertThat(respuesta.opciones()).containsExactly("Consultar restricciones", "Crear restricción en obra", "Consultar estado de obra");
    }

    private TelegramController.Respuesta enviar(long evento, String texto) {
        return flujo.responder(10L, 8L, "Ana", evento, texto, ejecutar);
    }
}
