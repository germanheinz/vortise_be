package com.vortise.gestion.application.service;

import com.vortise.gestion.application.mapper.ObraTareaMapper;
import com.vortise.gestion.application.dto.ObraTareaCreateDto;
import com.vortise.gestion.application.dto.ObraTareaDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.TipoNodoRubro;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.rule.EstructuraRubros;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObraTareaService {
    private final ObraTareaRepository obraTareaRepository;
    private final ProyectoRepository proyectoRepository;
    private final CertificadoBloqueos bloqueos;
    private final CurrentIdentity identity;

    @Autowired
    public ObraTareaService(ObraTareaRepository obraTareaRepository, ProyectoRepository proyectoRepository,
            CertificadoBloqueos bloqueos, CurrentIdentity identity) {
        this.obraTareaRepository = obraTareaRepository;
        this.proyectoRepository = proyectoRepository;
        this.bloqueos = bloqueos;
        this.identity = identity;
    }

    public ObraTareaService(ObraTareaRepository obraTareaRepository, ProyectoRepository proyectoRepository) {
        this(obraTareaRepository, proyectoRepository, null, null);
    }

    @Transactional
    public ObraTareaDto create(Long proyectoId, ObraTareaCreateDto dto) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));

        ObraTarea tarea = new ObraTarea();
        tarea.setProyecto(proyecto);
        applyActivity(tarea, dto);
        aplicarDatosDeCarga(tarea, dto);
        tarea.setRegistradoEn(LocalDateTime.now());
        if (identity != null) tarea.setRegistradoPorUsuarioId(identity.usuarioId());

        return ObraTareaMapper.toDto(obraTareaRepository.save(tarea));
    }

    @Transactional
    public ObraTareaDto update(Long proyectoId, Long tareaId, ObraTareaCreateDto dto) {
        ObraTarea tarea = obraTareaRepository.findById(tareaId)
            .filter(item -> item.getProyecto().getId().equals(proyectoId))
            .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));
        // Jefatura completes the loads it registered; editing someone else's load is for Gerencia/Coordinación.
        if (identity != null && identity.isJefeDeObra() && !identity.usuarioId().equals(tarea.getRegistradoPorUsuarioId())) {
            throw new com.vortise.gestion.application.exception.ReglaDeEstadoException("Solo podés modificar las tareas que cargaste vos.");
        }
        if (bloqueos != null) bloqueos.validarAvance(proyectoId, tarea.getActividad(), tarea.getFecha());
        applyActivity(tarea, dto);
        aplicarDatosDeCarga(tarea, dto);

        return ObraTareaMapper.toDto(obraTareaRepository.save(tarea));
    }

    @Transactional(readOnly = true)
    public List<ObraTareaDto> listar(Long proyectoId) {
        return obraTareaRepository.findByProyectoId(proyectoId).stream().map(ObraTareaMapper::toDto).toList();
    }

    public void delete(Long proyectoId, Long tareaId) {
        ObraTarea tarea = obraTareaRepository.findById(tareaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));

        if (tarea.getProyecto() == null || !proyectoId.equals(tarea.getProyecto().getId())) {
            throw new SolicitudInvalidaException("La tarea no pertenece a la obra indicada");
        }
        if (bloqueos != null) bloqueos.validarAvance(proyectoId, tarea.getActividad(), tarea.getFecha());

        obraTareaRepository.deleteById(tareaId);
    }

    private static void aplicarDatosDeCarga(ObraTarea tarea, ObraTareaCreateDto dto) {
        tarea.setNombre(dto.nombre());
        tarea.setSector(dto.sector());
        tarea.setUnidad(dto.unidad());
        tarea.setRubro(dto.rubro());
        tarea.setCumplimiento(dto.cumplimiento());
        tarea.setCausaNoCumplimiento(dto.causaNoCumplimiento());
        tarea.setComentario(dto.comentario());
        tarea.setHorasOficiales(controlaHoras(tarea.getProyecto(), dto) ? dto.horasOficiales() : null);
        tarea.setHorasAyudantes(controlaHoras(tarea.getProyecto(), dto) ? dto.horasAyudantes() : null);
        tarea.setCantidadReal(dto.cantidadReal());
        tarea.setDesencofrado(Boolean.TRUE.equals(dto.desencofrado()));
        tarea.setMedidaCorrectiva(dto.medidaCorrectiva());
        tarea.setDescripcion(dto.descripcion());
        tarea.setFecha(dto.fecha());
        if (dto.dias() != null || tarea.getId() == null) {
            tarea.setDias(dto.dias());
        }
        aplicarEtiquetasDelRubro(tarea, dto.rubroId());
    }

    private static void aplicarEtiquetasDelRubro(ObraTarea tarea, Long rubroId) {
        var actividad = tarea.getProyecto().getRubros().stream()
                .filter(rubro -> rubroId.equals(rubro.getId()))
                .findFirst()
                .orElseThrow();
        tarea.setRubro(actividad.getRubro());
        tarea.setUnidad(actividad.getUnidad());
        tarea.setNivel(actividad.getTipoNodo() == TipoNodoRubro.NIVEL ? EstructuraRubros.nombreDe(actividad) : actividad.getNivel());
    }

    /** The obra does not control labor hours of subcontracted activities. */
    private static boolean esSubcontrato(com.vortise.gestion.domain.model.RubroObra rubro) {
        return "SUBCONTRATO".equals(rubro.getTipoContratista());
    }

    private static boolean controlaHoras(Proyecto proyecto, ObraTareaCreateDto dto) {
        return proyecto.getRubros().stream().filter(item -> dto.rubroId().equals(item.getId()))
            .findFirst().map(item -> !esSubcontrato(item)).orElse(true);
    }

    private void applyActivity(ObraTarea tarea, ObraTareaCreateDto dto) {
        if (dto.rubroId() == null || dto.fecha() == null) {
            throw new SolicitudInvalidaException("Seleccioná una actividad y una fecha");
        }
        var rubro = tarea.getProyecto().getRubros().stream()
            .filter(item -> dto.rubroId().equals(item.getId())).findFirst()
            .orElseThrow(() -> new RecursoNoEncontradoException("La actividad no pertenece a la obra"));
        boolean sameActivity = tarea.getActividad() != null && rubro.getId() != null && rubro.getId().equals(tarea.getActividad().getId());
        if (!sameActivity && !EstructuraRubros.admiteCargaDeAvance(rubro, tarea.getProyecto().getRubros())) {
            throw new SolicitudInvalidaException("Seleccioná una actividad sin subdivisiones");
        }
        if (Boolean.TRUE.equals(dto.desencofrado()) && dto.cantidadReal() != null) {
            throw new SolicitudInvalidaException("El desencofrado solo imputa horas: no admite cantidad ejecutada");
        }
        // La cantidad ejecutada es opcional: sin ella la carga queda como actividad en proceso.
        if (dto.cantidadReal() != null && (!Double.isFinite(dto.cantidadReal()) || dto.cantidadReal() < 0)) {
            throw new SolicitudInvalidaException("Cantidad y horas reales deben ser números no negativos");
        }
        if (!esSubcontrato(rubro)) {
            for (Double value : new Double[]{dto.horasOficiales(), dto.horasAyudantes()}) {
                if (value == null || !Double.isFinite(value) || value < 0) {
                    throw new SolicitudInvalidaException("Cantidad y horas reales deben ser números no negativos");
                }
            }
            if (!Double.isFinite(dto.horasOficiales() + dto.horasAyudantes())) {
                throw new SolicitudInvalidaException("Total de horas fuera de rango");
            }
        }
        if (dto.dias() != null && (dto.dias() < 1 || dto.dias() > ObraTareaCreateDto.MAX_DIAS)) {
            throw new SolicitudInvalidaException("Los días deben estar entre 1 y " + ObraTareaCreateDto.MAX_DIAS);
        }
        if (dto.cantidadPrevista() != null && (!Double.isFinite(dto.cantidadPrevista()) || dto.cantidadPrevista() < 0)) {
            throw new SolicitudInvalidaException("Cantidad prevista inválida");
        }
        if (bloqueos != null) bloqueos.validarAvance(tarea.getProyecto().getId(), rubro, dto.fecha());
        tarea.setActividad(rubro);
    }
}
