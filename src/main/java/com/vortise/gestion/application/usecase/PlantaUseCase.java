package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PlantaDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlantaUseCase {
    private final PlantaRepository plantas;
    private final ProyectoRepository proyectos;
    private final CurrentIdentity identity;
    public PlantaUseCase(PlantaRepository plantas, ProyectoRepository proyectos, CurrentIdentity identity) {
        this.plantas = plantas; this.proyectos = proyectos; this.identity = identity;
    }
    @Transactional(readOnly = true)
    public List<PlantaDto> getAll(Long proyectoId) {
        requireProject(proyectoId, false);
        return plantas.findByProyectoId(proyectoId).stream().map(PlantaUseCase::toDto).toList();
    }
    public PlantaDto create(Long proyectoId, PlantaDto dto) {
        Proyecto proyecto = requireProject(proyectoId, true);
        Planta planta = new Planta();
        planta.setProyecto(proyecto);
        apply(planta, dto);
        return toDto(plantas.save(planta));
    }
    public PlantaDto update(Long proyectoId, Long id, PlantaDto dto) {
        requireProject(proyectoId, true);
        Planta planta = requireOwned(proyectoId, id);
        apply(planta, dto);
        return toDto(plantas.save(planta));
    }
    public void delete(Long proyectoId, Long id) {
        Proyecto proyecto = requireProject(proyectoId, true);
        Planta planta = requireOwned(proyectoId, id);
        // Keep the aggregate consistent: cascade=PERSIST must not re-save a removed child.
        proyecto.getPlantas().removeIf(child -> id.equals(child.getId()));
        plantas.deleteById(planta.getId());
    }
    private Proyecto requireProject(Long id, boolean lock) {
        Proyecto proyecto = (lock ? proyectos.bloquearPorId(id) : proyectos.findById(id))
            .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
        if (proyecto.getEmpresaRelacionada() == null || proyecto.getEmpresaRelacionada().getId() == null) throw new AccessDeniedException("Recurso sin empresa asignada");
        identity.requireEmpresa(proyecto.getEmpresaRelacionada().getId());
        return proyecto;
    }
    private Planta requireOwned(Long proyectoId, Long id) {
        Planta planta = plantas.findById(id).orElseThrow(PlantaUseCase::missing);
        if (planta.getProyecto() == null || !proyectoId.equals(planta.getProyecto().getId())) throw missing();
        return planta;
    }
    private static RecursoNoEncontradoException missing() { return new RecursoNoEncontradoException("Planta no encontrada"); }
    private static void apply(Planta p, PlantaDto dto) {
        p.setNivel(dto.nivel()); p.setSuperficie(dto.superficie()); p.setSupCubierta(dto.supCubierta());
        p.setSupDescubierta(dto.supDescubierta()); p.setSupCalculada(dto.supCalculada()); p.setCoefPlantaTipo(dto.coefPlantaTipo());
    }
    private static PlantaDto toDto(Planta p) {
        return new PlantaDto(p.getId(), p.getProyecto().getId(), p.getNivel(), p.getSuperficie(), p.getSupCubierta(), p.getSupDescubierta(), p.getSupCalculada(), p.getCoefPlantaTipo());
    }
}
