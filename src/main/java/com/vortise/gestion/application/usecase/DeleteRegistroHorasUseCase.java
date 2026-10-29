package com.vortise.gestion.application.usecase;

import org.springframework.stereotype.Service;

@Service
public class DeleteRegistroHorasUseCase {
    private final RegistroHorasService service;
    public DeleteRegistroHorasUseCase(RegistroHorasService service) { this.service = service; }
    public void execute(Long id) { service.delete(id); }
}
