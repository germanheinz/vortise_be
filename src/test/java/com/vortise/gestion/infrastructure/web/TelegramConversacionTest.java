package com.vortise.gestion.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.vortise.gestion.application.usecase.ConsultarRestriccionesObraUseCase;
import com.vortise.gestion.application.usecase.ReporteObraUseCase;
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
    private final TelegramConversacion flujo = new TelegramConversacion(proyectos, empresas, identidad,
        mock(ReporteObraUseCase.class), mock(ConsultarRestriccionesObraUseCase.class));
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

    private TelegramController.Respuesta enviar(long evento, String texto) {
        return flujo.responder(10L, 8L, "Ana", evento, texto, ejecutar);
    }
}
