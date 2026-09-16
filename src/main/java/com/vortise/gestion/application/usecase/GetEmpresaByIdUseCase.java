package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

@Service
public class GetEmpresaByIdUseCase {
    private final EmpresaRepository empresaRepository;

    public GetEmpresaByIdUseCase(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public EmpresaDto execute(Long id) {
        return EmpresaMapper.toDto(empresaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada")));
    }
}
