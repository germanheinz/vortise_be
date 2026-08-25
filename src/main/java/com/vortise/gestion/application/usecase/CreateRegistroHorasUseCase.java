package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RegistroHorasCreateDto;
import com.vortise.gestion.application.dto.RegistroHorasDto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RegistroHoras;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class CreateRegistroHorasUseCase {
    private final RegistroHorasRepository registroHorasRepository;
    private final ProyectoRepository proyectoRepository;

    public CreateRegistroHorasUseCase(RegistroHorasRepository registroHorasRepository, ProyectoRepository proyectoRepository) {
        this.registroHorasRepository = registroHorasRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public RegistroHorasDto execute(Long proyectoId, RegistroHorasCreateDto dto) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        RegistroHoras registroHoras = new RegistroHoras();
        apply(dto, registroHoras);
        registroHoras.setProyecto(proyecto);
        RegistroHoras saved = registroHorasRepository.save(registroHoras);
        recalcularHorasReales(proyectoId);
        return RegistroHorasMapper.toDto(saved);
    }

    private void apply(RegistroHorasCreateDto dto, RegistroHoras registroHoras) {
        if (dto.fin().isBefore(dto.inicio()) || dto.fin().isEqual(dto.inicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior al inicio");
        }

        registroHoras.setNumeroRubro(dto.numeroRubro());
        registroHoras.setRubro(dto.rubro());
        registroHoras.setSubRubro(dto.subRubro());
        registroHoras.setResponsable(dto.responsable());
        registroHoras.setInicio(dto.inicio());
        registroHoras.setFin(dto.fin());
        registroHoras.setHoras(roundHours(Duration.between(dto.inicio(), dto.fin()).toMinutes() / 60.0));
        registroHoras.setDescripcion(dto.descripcion());
    }

    private void recalcularHorasReales(Long proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        double horasReales = registroHorasRepository.findByProyectoId(proyectoId).stream()
            .mapToDouble(registro -> registro.getHoras() == null ? 0.0 : registro.getHoras())
            .sum();
        proyecto.setHorasReales(roundHours(horasReales));
        proyectoRepository.save(proyecto);
    }

    private double roundHours(double hours) {
        return Math.round(hours * 100.0) / 100.0;
    }
}