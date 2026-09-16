package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetObrasByEmpresaUseCase {
    private final EmpresaRepository empresaRepository;
    private final ProyectoRepository proyectoRepository;

    public GetObrasByEmpresaUseCase(EmpresaRepository empresaRepository, ProyectoRepository proyectoRepository) {
        this.empresaRepository = empresaRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public List<ProyectoDto> execute(Long empresaId) {
        if (empresaRepository.findById(empresaId).isEmpty()) {
            throw new IllegalArgumentException("Empresa no encontrada");
        }
        return proyectoRepository.findByEmpresaRelacionadaId(empresaId).stream()
            .map(ProyectoMapper::toSummaryDto)
            .toList();
    }
}