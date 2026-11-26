package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.RegistroHorasCreateDto;
import com.vortise.gestion.application.dto.RegistroHorasDto;
import com.vortise.gestion.application.dto.RegistroHorasUpdateDto;
import com.vortise.gestion.application.usecase.GetRegistroHorasByIdUseCase;
import com.vortise.gestion.application.usecase.GetRegistrosHorasUseCase;
import com.vortise.gestion.application.usecase.RegistroHorasService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RegistroHorasController {
    private final GetRegistrosHorasUseCase getRegistrosHorasUseCase;
    private final GetRegistroHorasByIdUseCase getRegistroHorasByIdUseCase;
    private final RegistroHorasService registroHorasService;

    public RegistroHorasController(
        GetRegistrosHorasUseCase getRegistrosHorasUseCase,
        GetRegistroHorasByIdUseCase getRegistroHorasByIdUseCase,
        RegistroHorasService registroHorasService
    ) {
        this.getRegistrosHorasUseCase = getRegistrosHorasUseCase;
        this.getRegistroHorasByIdUseCase = getRegistroHorasByIdUseCase;
        this.registroHorasService = registroHorasService;
    }

    @GetMapping("/proyectos/{proyectoId}/registro-horas")
    public ResponseEntity<List<RegistroHorasDto>> getAllByProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(getRegistrosHorasUseCase.execute(proyectoId));
    }

    @GetMapping("/registro-horas/{id}")
    public ResponseEntity<RegistroHorasDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getRegistroHorasByIdUseCase.execute(id));
    }

    @PostMapping("/proyectos/{proyectoId}/registro-horas")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.OBRA)
    public ResponseEntity<RegistroHorasDto> create(@PathVariable Long proyectoId, @Valid @RequestBody RegistroHorasCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registroHorasService.create(proyectoId, dto));
    }

    @PutMapping("/registro-horas/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.OBRA)
    public ResponseEntity<RegistroHorasDto> update(@PathVariable Long id, @Valid @RequestBody RegistroHorasUpdateDto dto) {
        return ResponseEntity.ok(registroHorasService.update(id, dto));
    }

    @DeleteMapping("/registro-horas/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.GERENTE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        registroHorasService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
