/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PatchMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.TareaDto;
import com.vortise.gestion.application.dto.TareaUpdateStatusDto;
import com.vortise.gestion.application.usecase.GetTareasUseCase;
import com.vortise.gestion.application.usecase.UpdateTareaStatusUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api"})
public class TareaController {
    private final GetTareasUseCase getTareasUseCase;
    private final UpdateTareaStatusUseCase updateTareaStatusUseCase;

    @GetMapping(value={"/proyectos/{proyectoId}/tareas"})
    public ResponseEntity<List<TareaDto>> getByProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(this.getTareasUseCase.execute(proyectoId));
    }

    @PatchMapping(value={"/tareas/{id}/status"})
    public ResponseEntity<TareaDto> updateStatus(@PathVariable Long id, @Valid @RequestBody TareaUpdateStatusDto dto) {
        return ResponseEntity.ok(this.updateTareaStatusUseCase.execute(id, dto));
    }

    public TareaController(GetTareasUseCase getTareasUseCase, UpdateTareaStatusUseCase updateTareaStatusUseCase) {
        this.getTareasUseCase = getTareasUseCase;
        this.updateTareaStatusUseCase = updateTareaStatusUseCase;
    }
}
