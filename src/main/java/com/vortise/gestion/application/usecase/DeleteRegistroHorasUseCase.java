package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RegistroHoras;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteRegistroHorasUseCase {
    private final RegistroHorasRepository registroHorasRepository;
    private final ProyectoRepository proyectoRepository;

    public DeleteRegistroHorasUseCase(RegistroHorasRepository registroHorasRepository, ProyectoRepository proyectoRepository) {
        this.registroHorasRepository = registroHorasRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public void execute(Long id) {
        RegistroHoras registroHoras = registroHorasRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Registro de horas no encontrado"));
        Long proyectoId = registroHoras.getProyecto().getId();
        registroHorasRepository.deleteById(id);
        recalcularHorasReales(proyectoId);
    }

    private void recalcularHorasReales(Long proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        double horasReales = registroHorasRepository.findByProyectoId(proyectoId).stream()
            .mapToDouble(registro -> registro.getHoras() == null ? 0.0 : registro.getHoras())
            .sum();
        proyecto.setHorasReales(Math.round(horasReales * 100.0) / 100.0);
        proyectoRepository.save(proyecto);
    }
}