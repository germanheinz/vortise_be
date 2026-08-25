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

import com.vortise.gestion.application.dto.CostoIndirectoDto;
import com.vortise.gestion.application.usecase.GetCostosIndirectosUseCase;
import com.vortise.gestion.application.usecase.SaveCostoIndirectoUseCase;
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
@RequestMapping(value={"/api/proyectos/{proyectoId}/costos-indirectos"})
public class CostoIndirectoController {
    private final GetCostosIndirectosUseCase getCostosIndirectosUseCase;
    private final SaveCostoIndirectoUseCase saveCostoIndirectoUseCase;

    @GetMapping
    public ResponseEntity<List<CostoIndirectoDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(this.getCostosIndirectosUseCase.execute(proyectoId));
    }

    @PostMapping
    public ResponseEntity<CostoIndirectoDto> create(@PathVariable Long proyectoId, @Valid @RequestBody CostoIndirectoDto dto) {
        CostoIndirectoDto withProject = new CostoIndirectoDto(dto.id(), proyectoId, dto.concepto(), dto.monto(), dto.mes());
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.saveCostoIndirectoUseCase.execute(withProject));
    }

    public CostoIndirectoController(GetCostosIndirectosUseCase getCostosIndirectosUseCase, SaveCostoIndirectoUseCase saveCostoIndirectoUseCase) {
        this.getCostosIndirectosUseCase = getCostosIndirectosUseCase;
        this.saveCostoIndirectoUseCase = saveCostoIndirectoUseCase;
    }
}
