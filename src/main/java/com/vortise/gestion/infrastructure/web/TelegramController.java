package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.service.VinculacionTelegramService;
import com.vortise.gestion.infrastructure.security.Guards;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integraciones/telegram")
public class TelegramController {
    private final VinculacionTelegramService vincular;
    private final String secreto;
    private final TelegramComandos comandos;

    public TelegramController(VinculacionTelegramService vincular, TelegramComandos comandos,
            @Value("${app.integraciones.telegram.secreto:}") String secreto) {
        this.vincular = vincular;
        this.secreto = secreto;
        this.comandos = comandos;
    }

    @GetMapping("/vinculo")
    @PreAuthorize(Guards.GERENCIA_CHAT)
    public VinculacionTelegramService.Estado estado() {
        return vincular.estado();
    }

    @PostMapping("/vinculo")
    @PreAuthorize(Guards.GERENCIA_CHAT)
    public VinculacionTelegramService.Inicio iniciar() {
        return vincular.iniciar();
    }

    @PostMapping("/vinculo/confirmar")
    @PreAuthorize(Guards.GERENCIA_CHAT)
    public VinculacionTelegramService.Estado confirmar(@Valid @RequestBody Confirmacion datos) {
        return vincular.confirmar(datos.chatId());
    }

    @DeleteMapping("/vinculo")
    @PreAuthorize(Guards.GERENCIA_CHAT)
    public void desvincular() {
        vincular.desvincular();
    }

    @PostMapping("/evento")
    public Respuesta recibir(@RequestHeader(value = "X-Telegram-Relay-Secret", required = false) String recibido,
            @Valid @RequestBody Evento evento) {
        if (secreto.length() < 32 || recibido == null || !MessageDigest.isEqual(
                secreto.getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Servicio de Telegram no autorizado");
        }
        if (!"private".equals(evento.tipoChat()) || !evento.chatId().equals(evento.usuarioTelegramId())) {
            return new Respuesta(null);
        }
        String mensaje = evento.texto().strip();
        if (!mensaje.matches("/start [A-Za-z0-9_-]{32}")) {
            return comandos.ejecutar(evento.chatId(), evento.eventoId(), mensaje);
        }
        boolean recibidoPorVortise = vincular.recibir(mensaje.substring(7), evento.chatId());
        return new Respuesta(recibidoPorVortise
            ? "Volvé a Vórtise y confirmá la vinculación del chat " + evento.chatId() + "."
            : "El código venció o ya se usó. Generá otro desde Vórtise.");
    }

    public record Confirmacion(@NotNull @Positive Long chatId) { }
    public record Evento(@NotNull @Positive Long chatId, @NotNull @Positive Long usuarioTelegramId,
            @NotNull String tipoChat, @NotNull @Positive Long eventoId, @NotBlank @Size(max = 4096) String texto) { }
    /** `enlaces` are https pages of the web app; n8n renders them as URL buttons. */
    public record Respuesta(String mensaje, java.util.List<String> opciones, Long menuEvento, java.util.List<Enlace> enlaces) {
        public Respuesta(String mensaje) { this(mensaje, java.util.List.of(), null, java.util.List.of()); }
        public Respuesta(String mensaje, java.util.List<String> opciones) { this(mensaje, opciones, null, java.util.List.of()); }
        public Respuesta(String mensaje, java.util.List<String> opciones, Long menuEvento) { this(mensaje, opciones, menuEvento, java.util.List.of()); }
    }
    public record Enlace(String texto, String url) { }
}
