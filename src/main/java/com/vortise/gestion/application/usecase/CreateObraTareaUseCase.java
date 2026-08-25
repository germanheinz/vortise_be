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

        return toDto(obraTareaRepository.save(tarea));
    }

    static ObraTareaDto toDto(ObraTarea tarea) {
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
            tarea.getComentario()
        );
    }
}