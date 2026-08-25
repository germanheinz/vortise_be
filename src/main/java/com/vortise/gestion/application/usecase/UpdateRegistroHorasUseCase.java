package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RegistroHorasDto;
import com.vortise.gestion.application.dto.RegistroHorasUpdateDto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RegistroHoras;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class UpdateRegistroHorasUseCase {
    private final RegistroHorasRepository registroHorasRepository;
    private final ProyectoRepository proyectoRepository;

    public UpdateRegistroHorasUseCase(RegistroHorasRepository registroHorasRepository, ProyectoRepository proyectoRepository) {
        this.registroHorasRepository = registroHorasRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public RegistroHorasDto execute(Long id, RegistroHorasUpdateDto dto) {
        RegistroHoras registroHoras = registroHorasRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Registro de horas no encontrado"));

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

        RegistroHoras saved = registroHorasRepository.save(registroHoras);
        Long proyectoId = saved.getProyecto().getId();
        recalcularHorasReales(proyectoId);
        return RegistroHorasMapper.toDto(saved);
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