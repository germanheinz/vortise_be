package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ObraTareaCreateDto;
import com.vortise.gestion.application.dto.ObraTareaDto;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateObraTareaUseCase {
    private final ObraTareaRepository obraTareaRepository;
    private final ProyectoRepository proyectoRepository;

    public CreateObraTareaUseCase(ObraTareaRepository obraTareaRepository, ProyectoRepository proyectoRepository) {
        this.obraTareaRepository = obraTareaRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public ObraTareaDto execute(Long proyectoId, ObraTareaCreateDto dto) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        ObraTarea tarea = new ObraTarea();
        tarea.setProyecto(proyecto);
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

        return toDto(obraTareaRepository.save(tarea));
    }

    public ObraTareaDto update(Long proyectoId, Long tareaId, ObraTareaCreateDto dto) {
        ObraTarea tarea = obraTareaRepository.findById(tareaId)
            .filter(item -> item.getProyecto().getId().equals(proyectoId))
            .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
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
        return toDto(obraTareaRepository.save(tarea));
    }

    static ObraTareaDto toDto(ObraTarea tarea) {
        var rubroBase = tarea.getProyecto().getRubros() == null ? null : tarea.getProyecto().getRubros().stream()
            .filter(item -> item.getRubro() != null && item.getRubro().equalsIgnoreCase(tarea.getRubro()))
            .findFirst().orElse(null);
        double horasTotales = (tarea.getHorasOficiales() == null ? 0 : tarea.getHorasOficiales()) + (tarea.getHorasAyudantes() == null ? 0 : tarea.getHorasAyudantes());
        Double productividadPresupuesto = rubroBase == null ? null : rubroBase.getProductividad();
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
            tarea.getPorcentajeCumplimiento(),
            tarea.getCumplimiento(),
            tarea.getCausaNoCumplimiento(),
            tarea.getComentario(),
            tarea.getHorasOficiales(),
            tarea.getHorasAyudantes(),
            tarea.getCantidadPrevista(),
            tarea.getCantidadReal(),
            tarea.getMedidaCorrectiva(),
            tarea.getDescripcion(),
            tarea.getFecha(),
            productividadPresupuesto,
            productividadReal,
            rendimiento,
            desvio
        );
    }

    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
}