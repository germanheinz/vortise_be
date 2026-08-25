package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.ObraTareaCreateDto;
import com.vortise.gestion.application.dto.ObraTareaDto;
import com.vortise.gestion.application.usecase.CreateObraTareaUseCase;
import com.vortise.gestion.application.usecase.DeleteObraTareaUseCase;
import com.vortise.gestion.application.usecase.GetObraTareasUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/obra-tareas")
public class ObraTareaController {
    private final GetObraTareasUseCase getObraTareasUseCase;
    private final CreateObraTareaUseCase createObraTareaUseCase;
    private final DeleteObraTareaUseCase deleteObraTareaUseCase;

    public ObraTareaController(
        GetObraTareasUseCase getObraTareasUseCase,
        CreateObraTareaUseCase createObraTareaUseCase,
        DeleteObraTareaUseCase deleteObraTareaUseCase
    ) {
        this.getObraTareasUseCase = getObraTareasUseCase;
        this.createObraTareaUseCase = createObraTareaUseCase;
        this.deleteObraTareaUseCase = deleteObraTareaUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ObraTareaDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(getObraTareasUseCase.execute(proyectoId));
    }

    @PostMapping
    public ResponseEntity<ObraTareaDto> create(@PathVariable Long proyectoId, @Valid @RequestBody ObraTareaCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createObraTareaUseCase.execute(proyectoId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long proyectoId, @PathVariable Long id) {
        deleteObraTareaUseCase.execute(proyectoId, id);
        return ResponseEntity.noContent().build();
    }
}