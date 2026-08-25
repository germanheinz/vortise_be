/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.EntregaDto;
import com.vortise.gestion.application.usecase.GetEntregasUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/proyectos/{proyectoId}/entregas"})
public class EntregaController {
    private final GetEntregasUseCase getEntregasUseCase;

    @GetMapping
    public ResponseEntity<List<EntregaDto>> getByProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(this.getEntregasUseCase.execute(proyectoId));
    }

    public EntregaController(GetEntregasUseCase getEntregasUseCase) {
        this.getEntregasUseCase = getEntregasUseCase;
    }
}
