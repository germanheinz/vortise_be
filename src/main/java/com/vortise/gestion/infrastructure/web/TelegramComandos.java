package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.usecase.ConsultarRestriccionesObraUseCase;
import com.vortise.gestion.application.usecase.CrearRestriccionChatUseCase;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.infrastructure.persistence.JpaVinculoTelegramRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import io.jsonwebtoken.Jwts;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TelegramComandos {
    private final JpaVinculoTelegramRepository vinculos;
    private final UsuarioEmpresaRepository usuarios;
    private final ProyectoRepository proyectos;
    private final CurrentTenantService identidad;
    private final ConsultarRestriccionesObraUseCase consultar;
    private final CrearRestriccionChatUseCase crear;
    private final Validator validator;
    private final TelegramConversacion conversacion;
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Accepts DD/MM/AAAA (as the guided flow asks) and AAAA-MM-DD. */
    static LocalDate fechaDe(String texto) {
        return texto.contains("/") ? LocalDate.parse(texto, FECHA) : LocalDate.parse(texto);
    }

    public TelegramComandos(JpaVinculoTelegramRepository vinculos, UsuarioEmpresaRepository usuarios,
            ProyectoRepository proyectos, CurrentTenantService identidad,
            ConsultarRestriccionesObraUseCase consultar, CrearRestriccionChatUseCase crear, Validator validator, TelegramConversacion conversacion) {
        this.vinculos = vinculos;
        this.usuarios = usuarios;
        this.proyectos = proyectos;
        this.identidad = identidad;
        this.consultar = consultar;
        this.crear = crear;
        this.validator = validator;
        this.conversacion = conversacion;
    }

    public TelegramController.Respuesta ejecutar(long chatId, long eventoId, String mensaje) {
        var usuario = vinculos.findByChatConfirmado(chatId)
            .flatMap(vinculo -> usuarios.findById(vinculo.getUsuarioId()))
            .filter(cuenta -> cuenta.isActivo() && (cuenta.getRol() == RolEmpresa.ADMINISTRADOR || cuenta.isSuperAdmin()) && cuenta.getEmpresa() != null);
        if (usuario.isEmpty()) return new TelegramController.Respuesta("Vinculá este chat desde tu sesión de Vórtise.");
        var cuenta = usuario.get();
        var anterior = SecurityContextHolder.getContext();
        var delegada = SecurityContextHolder.createEmptyContext();
        var auth = new UsernamePasswordAuthenticationToken(cuenta.getEmail(), null,
            List.of(new SimpleGrantedAuthority(cuenta.isSuperAdmin() ? "ROLE_SUPER_ADMIN" : "ROLE_ADMINISTRADOR")));
        auth.setDetails(Jwts.claims().add("userId", cuenta.getId()).add("empresaId", cuenta.getEmpresa().getId()).build());
        delegada.setAuthentication(auth);
        SecurityContextHolder.setContext(delegada);
        try {
            String texto = mensaje.strip();
            if (texto.startsWith("/restricciones ") || texto.startsWith("/nueva_restriccion ")) {
                return new TelegramController.Respuesta(ejecutarAutorizado(chatId, eventoId, texto), TelegramConversacion.MENU);
            }
            return conversacion.responder(chatId, cuenta.getId(), cuenta.getNombre(), eventoId, texto,
                comando -> ejecutarAutorizado(chatId, eventoId, comando));
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }

    private String ejecutarAutorizado(long chatId, long eventoId, String mensaje) {
        String[] partes = mensaje.split("\\s+", 4);
        String comando = partes[0];
        if (!"/restricciones".equals(comando) && !"/nueva_restriccion".equals(comando)) {
            return "Consultá con /restricciones ID_OBRA o creá con /nueva_restriccion ID_OBRA DD/MM/AAAA descripción.";
        }
        Long proyectoId;
        try {
            proyectoId = partes.length > 1 ? Long.valueOf(partes[1]) : null;
        } catch (NumberFormatException ex) {
            proyectoId = null;
        }
        if (proyectoId == null || proyectoId <= 0) return "Indicá el ID de obra que aparece en la URL de Vórtise.";
        var obra = proyectos.findById(proyectoId);
        if (obra.isEmpty() || obra.get().getEmpresaRelacionada() == null
                || !identidad.canAccessEmpresa(obra.get().getEmpresaRelacionada().getId())) {
            return "No encontramos esa obra entre las que tenés autorizadas.";
        }
        if ("/restricciones".equals(comando)) {
            var datos = consultar.execute(proyectoId, null, ConsultarRestriccionesObraUseCase.Filtro.ABIERTAS, null);
            return datos.obra() + "\nAbiertas: " + datos.abiertas() + "\nVencidas: " + datos.vencidas()
                + "\nPrioridad alta: " + datos.altaPrioridad();
        }
        if (partes.length < 4) return "Indicá la fecha límite y la descripción: /nueva_restriccion ID_OBRA DD/MM/AAAA descripción.";
        LocalDate fecha;
        try {
            fecha = fechaDe(partes[2]);
        } catch (DateTimeParseException ex) {
            return "Ingresá la fecha límite como DD/MM/AAAA.";
        }
        var solicitud = new CrearRestriccionChatUseCase.Solicitud(partes[3], fecha);
        if (!validator.validate(solicitud).isEmpty()) return "Ingresá una descripción de hasta 255 caracteres.";
        try {
            var resultado = crear.execute(proyectoId, "telegram:" + chatId + ":" + eventoId, solicitud);
            return "Restricción #" + resultado.id() + " guardada en " + resultado.obra()
                + ". Fecha límite: " + FECHA.format(resultado.fechaMaximaEntrega());
        } catch (com.vortise.gestion.application.exception.SolicitudInvalidaException ex) {
            return ex.getMessage();
        }
    }
}
