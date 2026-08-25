package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PlantaCreateDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UpdateProyectoUseCase {
    private final ProyectoRepository proyectoRepository;

    public UpdateProyectoUseCase(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    public ProyectoDto execute(Long proyectoId, ProyectoCreateDto dto) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        proyecto.setNombre(dto.nombre());
        proyecto.setDescripcion(dto.descripcion());
        proyecto.setPresupuestoUsd(dto.presupuestoUsd());
        proyecto.setDireccion(dto.direccion());
        proyecto.setNumeroProyecto(dto.numeroProyecto());
        proyecto.setEmpresa(dto.empresa());
        proyecto.setHorasPrevistas(dto.horasPrevistas());
        proyecto.setHorasReales(dto.horasReales());

        // Keep managed collection references to satisfy orphanRemoval semantics.
        if (proyecto.getPlantas() == null) {
            proyecto.setPlantas(new ArrayList<>());
        }
        proyecto.getPlantas().clear();
        proyecto.getPlantas().addAll(mapPlantas(dto.plantas(), proyecto));

        if (proyecto.getRubros() == null) {
            proyecto.setRubros(new ArrayList<>());
        }
        proyecto.getRubros().clear();
        proyecto.getRubros().addAll(mapRubros(dto.rubros(), proyecto));

        Proyecto saved = proyectoRepository.save(proyecto);
        return ProyectoMapper.toDetailDto(saved);
    }

    private List<Planta> mapPlantas(List<PlantaCreateDto> plantas, Proyecto proyecto) {
        if (plantas == null) {
            return new ArrayList<>();
        }

        List<Planta> result = new ArrayList<>();
        for (PlantaCreateDto plantaDto : plantas) {
            Planta planta = new Planta();
            planta.setProyecto(proyecto);
            planta.setNivel(plantaDto.nivel());
            planta.setSuperficie(plantaDto.superficie());
            planta.setSupCubierta(plantaDto.supCubierta());
            planta.setSupDescubierta(plantaDto.supDescubierta());
            planta.setSupCalculada(plantaDto.supCalculada());
            planta.setCoefPlantaTipo(plantaDto.coefPlantaTipo());
            result.add(planta);
        }
        return result;
    }

    private List<RubroObra> mapRubros(List<RubroCreateDto> rubros, Proyecto proyecto) {
        if (rubros == null) {
            return new ArrayList<>();
        }

        List<RubroObra> result = new ArrayList<>();
        for (RubroCreateDto rubroDto : rubros) {
            RubroObra rubro = new RubroObra();
            rubro.setProyecto(proyecto);
            rubro.setnRubro(rubroDto.nRubro());
            rubro.setRubro(rubroDto.rubro());
            rubro.setSubNumeroRubro(rubroDto.subNumeroRubro());
            rubro.setSubRubro(rubroDto.subRubro());
            rubro.setCantidad(rubroDto.cantidad());
            rubro.setUnidad(rubroDto.unidad());
            rubro.setProductividad(rubroDto.productividad());
            rubro.setEmpresa(rubroDto.empresa());
            result.add(rubro);
        }
        return result;
    }
}