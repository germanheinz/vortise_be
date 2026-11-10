package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.CertificadoObra;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.CertificadoItemCompletadoRepository;
import com.vortise.gestion.domain.repository.CertificadoObraRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps closed certificates and completed items untouched by changes made from Tareas and Actividades. */
@Service
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class CertificadoBloqueos {
    private final CertificadoObraRepository certificados;
    private final CertificadoItemCompletadoRepository completados;

    public CertificadoBloqueos(CertificadoObraRepository certificados, CertificadoItemCompletadoRepository completados) {
        this.certificados = certificados;
        this.completados = completados;
    }

    /** Company of an activity: the subcontractor id, or null for the own labor of the obra. */
    public static Long empresaDe(RubroObra rubro) {
        return "SUBCONTRATO".equals(rubro.getTipoContratista()) && rubro.getSubcontratista() != null ? rubro.getSubcontratista().getId() : null;
    }

    private static Long empresaDe(RubroCreateDto rubro) {
        return "SUBCONTRATO".equals(rubro.tipoContratista()) ? rubro.subcontratistaId() : null;
    }

    /** Rejects creating, editing or deleting a load that belongs to a closed certificate or to a completed item. */
    public void validarAvance(Long proyectoId, RubroObra actividad, LocalDate fecha) {
        if (actividad == null || actividad.getId() == null) return;
        if (completados.findByCertificadoProyectoId(proyectoId).stream().anyMatch(item -> actividad.getId().equals(item.getRubroId()))) {
            throw new SolicitudInvalidaException("La actividad fue completada en un certificado y sus avances no se pueden modificar");
        }
        LocalDate cierre = ultimoCorteCerrado(proyectoId, empresaDe(actividad));
        if (cierre != null && fecha != null && !fecha.isAfter(cierre)) {
            throw new SolicitudInvalidaException("La fecha pertenece a un certificado cerrado de la empresa (hasta el "
                + cierre.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }
    }

    /** {@code solicitado} is null when the activity is being removed. */
    public void validarCambioActividad(Long proyectoId, RubroObra existente, RubroCreateDto solicitado, List<ObraTarea> avances) {
        if (existente.getId() == null || !bloqueada(proyectoId, existente, avances)) return;
        if (solicitado == null) {
            throw new SolicitudInvalidaException("No se puede eliminar una actividad con avance en un certificado cerrado");
        }
        if (!Objects.equals(existente.getCantidad(), solicitado.cantidad()) || !Objects.equals(existente.getUnidad(), solicitado.unidad())
                || !Objects.equals(empresaDe(existente), empresaDe(solicitado))) {
            throw new SolicitudInvalidaException("No se puede cambiar la cantidad, la unidad ni la empresa de una actividad con avance en un certificado cerrado");
        }
    }

    private boolean bloqueada(Long proyectoId, RubroObra actividad, List<ObraTarea> avances) {
        LocalDate cierre = ultimoCorteCerrado(proyectoId, empresaDe(actividad));
        boolean conAvance = cierre != null && avances.stream().anyMatch(avance -> actividad.getId().equals(avance.getRubroId())
            && avance.getFecha() != null && !avance.getFecha().isAfter(cierre));
        return conAvance || completados.findByCertificadoProyectoId(proyectoId).stream()
            .anyMatch(item -> actividad.getId().equals(item.getRubroId()) && item.getCertificado().isCerrado());
    }

    private LocalDate ultimoCorteCerrado(Long proyectoId, Long empresa) {
        return certificados.findByProyectoIdOrderByFechaCorteAscIdAsc(proyectoId).stream()
            .filter(CertificadoObra::isCerrado).filter(item -> Objects.equals(item.getSubcontratistaId(), empresa))
            .map(CertificadoObra::getFechaCorte).max(LocalDate::compareTo).orElse(null);
    }
}
