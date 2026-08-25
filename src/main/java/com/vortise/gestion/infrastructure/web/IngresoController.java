/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.IngresoDto;
import com.vortise.gestion.application.usecase.GetIngresosUseCase;
import com.vortise.gestion.application.usecase.SaveIngresoUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/proyectos/{proyectoId}/ingresos"})
public class IngresoController {
    private final GetIngresosUseCase getIngresosUseCase;
    private final SaveIngresoUseCase saveIngresoUseCase;

    @GetMapping
    public ResponseEntity<List<IngresoDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(this.getIngresosUseCase.execute(proyectoId));
    }

    @PostMapping
    public ResponseEntity<IngresoDto> create(@PathVariable Long proyectoId, @Valid @RequestBody IngresoDto dto) {
        IngresoDto withProject = new IngresoDto(dto.id(), proyectoId, dto.concepto(), dto.monto(), dto.mes());
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.saveIngresoUseCase.execute(withProject));
    }

    public IngresoController(GetIngresosUseCase getIngresosUseCase, SaveIngresoUseCase saveIngresoUseCase) {
        this.getIngresosUseCase = getIngresosUseCase;
        this.saveIngresoUseCase = saveIngresoUseCase;
    }
}
