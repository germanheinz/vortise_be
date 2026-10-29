package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ObraTareaCreateDto;
import com.vortise.gestion.application.dto.ObraTareaDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateObraTareaUseCase {
    private final ObraTareaRepository obraTareaRepository;
    private final ProyectoRepository proyectoRepository;

    public CreateObraTareaUseCase(ObraTareaRepository obraTareaRepository, ProyectoRepository proyectoRepository) {
        this.obraTareaRepository = obraTareaRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public ObraTareaDto execute(Long proyectoId, ObraTareaCreateDto dto) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));

        ObraTarea tarea = new ObraTarea();
        tarea.setProyecto(proyecto);
        applyActivity(tarea, dto);
        tarea.setNombre(dto.nombre());
        tarea.setSector(dto.sector());
        tarea.setNivel(dto.nivel());
        tarea.setUnidad(dto.unidad());
        tarea.setRubro(dto.rubro());
        tarea.setPorcentajeCumplimiento(dto.porcentajeCumplimiento());
        tarea.setCumplimiento(dto.cumplimiento());
        tarea.setCausaNoCumplimiento(dto.causaNoCumplimiento());
        tarea.setComentario(dto.comentario());
        tarea.setHorasOficiales(dto.horasOficiales());
        tarea.setHorasAyudantes(dto.horasAyudantes());
        tarea.setCantidadPrevista(dto.cantidadPrevista());
        tarea.setCantidadReal(dto.cantidadReal());
        tarea.setMedidaCorrectiva(dto.medidaCorrectiva());
        tarea.setDescripcion(dto.descripcion());
        tarea.setFecha(dto.fecha());
        var actividad = tarea.getProyecto().getRubros().stream().filter(r -> dto.rubroId().equals(r.getId())).findFirst().orElseThrow();
        tarea.setRubro(actividad.getRubro());
        tarea.setUnidad(actividad.getUnidad());
        tarea.setNivel(actividad.getNivel());
        var calculated = PrevistoRealCalculator.calculate(actividad.getCantidad(), actividad.getHorasOficialesPrevistas(),
            actividad.getHorasAyudantesPrevistas(), tarea.getCantidadReal(), tarea.getHorasOficiales(), tarea.getHorasAyudantes(), null, null, null);
        tarea.setCantidadPrevista(calculated.cantidadEquivalente());
        tarea.setPorcentajeCumplimiento(calculated.cantidadEquivalente() != null && calculated.cantidadEquivalente() > 0
            ? round(tarea.getCantidadReal() / calculated.cantidadEquivalente() * 100) : null);

        return toDto(obraTareaRepository.save(tarea));
    }

    public ObraTareaDto update(Long proyectoId, Long tareaId, ObraTareaCreateDto dto) {
        ObraTarea tarea = obraTareaRepository.findById(tareaId)
            .filter(item -> item.getProyecto().getId().equals(proyectoId))
            .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));
        applyActivity(tarea, dto);
        tarea.setNombre(dto.nombre());
        tarea.setSector(dto.sector());
        tarea.setNivel(dto.nivel());
        tarea.setUnidad(dto.unidad());
        tarea.setRubro(dto.rubro());
        tarea.setPorcentajeCumplimiento(dto.porcentajeCumplimiento());
        tarea.setCumplimiento(dto.cumplimiento());
        tarea.setCausaNoCumplimiento(dto.causaNoCumplimiento());
        tarea.setComentario(dto.comentario());
        tarea.setHorasOficiales(dto.horasOficiales());
        tarea.setHorasAyudantes(dto.horasAyudantes());
        tarea.setCantidadPrevista(dto.cantidadPrevista());
        tarea.setCantidadReal(dto.cantidadReal());
        tarea.setMedidaCorrectiva(dto.medidaCorrectiva());
        tarea.setDescripcion(dto.descripcion());
        tarea.setFecha(dto.fecha());
        var actividad = tarea.getProyecto().getRubros().stream().filter(r -> dto.rubroId().equals(r.getId())).findFirst().orElseThrow();
        tarea.setRubro(actividad.getRubro());
        tarea.setUnidad(actividad.getUnidad());
        tarea.setNivel(actividad.getNivel());
        var calculated = PrevistoRealCalculator.calculate(actividad.getCantidad(), actividad.getHorasOficialesPrevistas(),
            actividad.getHorasAyudantesPrevistas(), tarea.getCantidadReal(), tarea.getHorasOficiales(), tarea.getHorasAyudantes(), null, null, null);
        tarea.setCantidadPrevista(calculated.cantidadEquivalente());
        tarea.setPorcentajeCumplimiento(calculated.cantidadEquivalente() != null && calculated.cantidadEquivalente() > 0
            ? round(tarea.getCantidadReal() / calculated.cantidadEquivalente() * 100) : null);
        return toDto(obraTareaRepository.save(tarea));
    }

    private void applyActivity(ObraTarea tarea, ObraTareaCreateDto dto) {
        if (dto.rubroId() == null || dto.fecha() == null) {
            throw new SolicitudInvalidaException("Seleccioná una actividad y una fecha");
        }
        var rubro = tarea.getProyecto().getRubros().stream()
            .filter(item -> dto.rubroId().equals(item.getId())).findFirst()
            .orElseThrow(() -> new RecursoNoEncontradoException("La actividad no pertenece a la obra"));
        for (Double value : new Double[]{dto.cantidadReal(), dto.horasOficiales(), dto.horasAyudantes()}) {
            if (value == null || !Double.isFinite(value) || value < 0) {
                throw new SolicitudInvalidaException("Cantidad y horas reales deben ser números no negativos");
            }
        }
        if (!Double.isFinite(dto.horasOficiales() + dto.horasAyudantes())) {
            throw new SolicitudInvalidaException("Total de horas fuera de rango");
        }
        if (dto.cantidadPrevista() != null && (!Double.isFinite(dto.cantidadPrevista()) || dto.cantidadPrevista() < 0)) {
            throw new SolicitudInvalidaException("Cantidad prevista inválida");
        }
        tarea.setActividad(rubro);
    }

    static ObraTareaDto toDto(ObraTarea tarea) {
        var rubroBase = tarea.getProyecto().getRubros() == null ? null : tarea.getProyecto().getRubros().stream()
            .filter(item -> item.getId() != null && item.getId().equals(tarea.getRubroId()))
            .findFirst().orElse(null);
        double horasTotales = (tarea.getHorasOficiales() == null ? 0 : tarea.getHorasOficiales()) + (tarea.getHorasAyudantes() == null ? 0 : tarea.getHorasAyudantes());
        Double productividadPresupuesto = rubroBase == null || rubroBase.getCantidad() == null || rubroBase.getCantidad() <= 0
            || rubroBase.getHorasOficialesPrevistas() == null || rubroBase.getHorasAyudantesPrevistas() == null ? null
            : (rubroBase.getHorasOficialesPrevistas() + rubroBase.getHorasAyudantesPrevistas()) / rubroBase.getCantidad();
        Double cantidadPrevista = productividadPresupuesto != null && productividadPresupuesto > 0
            && tarea.getHorasOficiales() != null && tarea.getHorasAyudantes() != null ? horasTotales / productividadPresupuesto : null;
        Double cumplimiento = cantidadPrevista != null && cantidadPrevista > 0 && tarea.getCantidadReal() != null
            ? round(tarea.getCantidadReal() / cantidadPrevista * 100) : null;
        Double productividadReal = tarea.getCantidadReal() == null || tarea.getCantidadReal() == 0 ? null : round(horasTotales / tarea.getCantidadReal());
        Double rendimiento = productividadPresupuesto == null || productividadReal == null || productividadPresupuesto == 0 ? null : round(productividadReal / productividadPresupuesto);
        Double desvio = rendimiento == null ? null : round((rendimiento - 1) * 100);
        return new ObraTareaDto(
            tarea.getId(),
            tarea.getProyecto().getId(),
            tarea.getNombre(),
            tarea.getSector(),
            tarea.getNivel(),
            tarea.getUnidad(),
            tarea.getRubro(),
            rubroBase == null ? tarea.getPorcentajeCumplimiento() : cumplimiento,
            tarea.getCumplimiento(),
            tarea.getCausaNoCumplimiento(),
            tarea.getComentario(),
            tarea.getHorasOficiales(),
            tarea.getHorasAyudantes(),
            rubroBase == null ? tarea.getCantidadPrevista() : cantidadPrevista,
            tarea.getCantidadReal(),
            tarea.getMedidaCorrectiva(),
            tarea.getDescripcion(),
            tarea.getFecha(),
            productividadPresupuesto,
            productividadReal,
            rendimiento,
            desvio,
            tarea.getRubroId()
        );
    }

    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
}