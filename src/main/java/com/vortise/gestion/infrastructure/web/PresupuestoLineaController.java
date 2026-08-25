/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.PresupuestoLineaDto;
import com.vortise.gestion.application.usecase.DeletePresupuestoLineaUseCase;
import com.vortise.gestion.application.usecase.GetPresupuestoLineasUseCase;
import com.vortise.gestion.application.usecase.SavePresupuestoLineaUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
@RequestMapping(value={"/api/proyectos/{proyectoId}/presupuesto"})
public class PresupuestoLineaController {
    private final GetPresupuestoLineasUseCase getPresupuestoLineasUseCase;
    private final SavePresupuestoLineaUseCase savePresupuestoLineaUseCase;
    private final DeletePresupuestoLineaUseCase deletePresupuestoLineaUseCase;

    @GetMapping
    public ResponseEntity<List<PresupuestoLineaDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(this.getPresupuestoLineasUseCase.execute(proyectoId));
    }

    @PostMapping
    public ResponseEntity<PresupuestoLineaDto> create(@PathVariable Long proyectoId, @Valid @RequestBody PresupuestoLineaDto dto) {
        PresupuestoLineaDto conProyecto = new PresupuestoLineaDto(null, proyectoId, dto.etapa(), dto.tipoSuperficie(), dto.funcion(), dto.sistema(), dto.m2(), dto.hhPorM2(), dto.precioPorHh(), dto.precioPorHhUsd(), dto.mes());
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.savePresupuestoLineaUseCase.execute(conProyecto));
    }

    @PutMapping(value={"/{id}"})
    public ResponseEntity<PresupuestoLineaDto> update(@PathVariable Long proyectoId, @PathVariable Long id, @Valid @RequestBody PresupuestoLineaDto dto) {
        PresupuestoLineaDto conId = new PresupuestoLineaDto(id, proyectoId, dto.etapa(), dto.tipoSuperficie(), dto.funcion(), dto.sistema(), dto.m2(), dto.hhPorM2(), dto.precioPorHh(), dto.precioPorHhUsd(), dto.mes());
        return ResponseEntity.ok(this.savePresupuestoLineaUseCase.execute(conId));
    }

    @DeleteMapping(value={"/{id}"})
    public ResponseEntity<Void> delete(@PathVariable Long proyectoId, @PathVariable Long id) {
        this.deletePresupuestoLineaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    public PresupuestoLineaController(GetPresupuestoLineasUseCase getPresupuestoLineasUseCase, SavePresupuestoLineaUseCase savePresupuestoLineaUseCase, DeletePresupuestoLineaUseCase deletePresupuestoLineaUseCase) {
        this.getPresupuestoLineasUseCase = getPresupuestoLineasUseCase;
        this.savePresupuestoLineaUseCase = savePresupuestoLineaUseCase;
        this.deletePresupuestoLineaUseCase = deletePresupuestoLineaUseCase;
    }
}
