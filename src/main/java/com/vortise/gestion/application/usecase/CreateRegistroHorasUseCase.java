package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RegistroHorasCreateDto;
import com.vortise.gestion.application.dto.RegistroHorasDto;
import org.springframework.stereotype.Service;

@Service
public class CreateRegistroHorasUseCase {
    private final RegistroHorasService service;
    public CreateRegistroHorasUseCase(RegistroHorasService service) { this.service = service; }
    public RegistroHorasDto execute(Long id, RegistroHorasCreateDto dto) { return service.create(id, dto); }
}
