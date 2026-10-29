package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RegistroHorasDto;
import com.vortise.gestion.application.dto.RegistroHorasUpdateDto;
import org.springframework.stereotype.Service;

@Service
public class UpdateRegistroHorasUseCase {
    private final RegistroHorasService service;
    public UpdateRegistroHorasUseCase(RegistroHorasService service) { this.service = service; }
    public RegistroHorasDto execute(Long id, RegistroHorasUpdateDto dto) { return service.update(id, dto); }
}
