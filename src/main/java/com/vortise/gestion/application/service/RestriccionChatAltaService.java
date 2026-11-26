package com.vortise.gestion.application.service;

import com.vortise.gestion.application.dto.RestriccionCreateDto;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.AltaRestriccionChat;
import com.vortise.gestion.domain.repository.AltaRestriccionChatRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestriccionChatAltaService {
    private static final ZoneId ZONA_OBRA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ProyectoRepository proyectos;
    private final AltaRestriccionChatRepository altas;
    private final RestriccionService crear;
    private final CurrentIdentity identidad;

    public RestriccionChatAltaService(ProyectoRepository proyectos, AltaRestriccionChatRepository altas,
            RestriccionService crear, CurrentIdentity identidad) {
        this.proyectos = proyectos;
        this.altas = altas;
        this.crear = crear;
        this.identidad = identidad;
    }

    @Transactional
    public Resultado execute(Long proyectoId, String clave, Solicitud solicitud) {
        if (!identidad.isAdministrator() && !identidad.isSuperAdmin()) throw new AccessDeniedException("El alta por chat requiere un administrador");
        if (clave == null || clave.isBlank() || clave.length() > 128) {
            throw new SolicitudInvalidaException("Idempotency-Key debe contener entre 1 y 128 caracteres");
        }
        Long usuarioId = identidad.usuarioId();
        var proyecto = proyectos.bloquearPorId(proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        if (proyecto.getEmpresaRelacionada() == null) throw new AccessDeniedException("Obra sin empresa asignada");
        Long empresaId = proyecto.getEmpresaRelacionada().getId();
        identidad.requireEmpresa(empresaId);

        String descripcion = solicitud.descripcion().strip();
        LocalDate fechaLimite = solicitud.fechaMaximaEntrega();
        String claveHash = hash(empresaId + ":" + proyectoId + ":" + usuarioId + ":" + clave);
        String solicitudHash = hash(fechaLimite + "\n" + descripcion);
        var anterior = altas.findById(claveHash);
        if (anterior.isPresent()) {
            var alta = anterior.get();
            if (!alta.coincideCon(solicitudHash)) {
                throw new ConflictoEstadoException("El identificador del envío ya corresponde a otra solicitud");
            }
            return new Resultado(alta.getRestriccionId(), proyectoId, proyecto.getNombre(), fechaLimite, true);
        }

        var creada = crear.crear(proyectoId, new RestriccionCreateDto(descripcion,
            null, null, null, null, null, null, null, LocalDate.now(ZONA_OBRA),
            fechaLimite, null, null, null, "PENDIENTE"));
        altas.save(new AltaRestriccionChat(claveHash, solicitudHash, proyectoId, usuarioId, creada.id()));
        return new Resultado(creada.id(), proyectoId, proyecto.getNombre(), fechaLimite, false);
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public record Solicitud(@NotBlank @Size(max = 255) String descripcion, @NotNull LocalDate fechaMaximaEntrega) { }

    public record Resultado(Long id, Long proyectoId, String obra, LocalDate fechaMaximaEntrega, boolean repetida) { }
}
