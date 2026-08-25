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

import com.vortise.gestion.application.dto.CashflowDto;
import com.vortise.gestion.application.usecase.GetCashflowUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/proyectos/{proyectoId}/cashflow"})
public class CashflowController {
    private final GetCashflowUseCase getCashflowUseCase;

    @GetMapping
    public ResponseEntity<CashflowDto> getCashflow(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(this.getCashflowUseCase.execute(proyectoId));
    }

    public CashflowController(GetCashflowUseCase getCashflowUseCase) {
        this.getCashflowUseCase = getCashflowUseCase;
    }
}
