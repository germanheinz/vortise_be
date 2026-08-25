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

import com.vortise.gestion.application.dto.GastoFijoDto;
import com.vortise.gestion.application.usecase.DeleteGastoFijoUseCase;
import com.vortise.gestion.application.usecase.GetGastosFijosUseCase;
import com.vortise.gestion.application.usecase.SaveGastoFijoUseCase;
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
@RequestMapping(value={"/api/gastos-fijos"})
public class GastoFijoController {
    private final GetGastosFijosUseCase getGastosFijosUseCase;
    private final SaveGastoFijoUseCase saveGastoFijoUseCase;
    private final DeleteGastoFijoUseCase deleteGastoFijoUseCase;

    @GetMapping
    public ResponseEntity<List<GastoFijoDto>> getAll() {
        return ResponseEntity.ok(this.getGastosFijosUseCase.execute());
    }

    @PostMapping
    public ResponseEntity<GastoFijoDto> create(@Valid @RequestBody GastoFijoDto dto) {
        GastoFijoDto sinId = new GastoFijoDto(null, dto.concepto(), dto.monto(), dto.categoria(), dto.mes());
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.saveGastoFijoUseCase.execute(sinId));
    }

    @PutMapping(value={"/{id}"})
    public ResponseEntity<GastoFijoDto> update(@PathVariable Long id, @Valid @RequestBody GastoFijoDto dto) {
        GastoFijoDto conId = new GastoFijoDto(id, dto.concepto(), dto.monto(), dto.categoria(), dto.mes());
        return ResponseEntity.ok(this.saveGastoFijoUseCase.execute(conId));
    }

    @DeleteMapping(value={"/{id}"})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.deleteGastoFijoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    public GastoFijoController(GetGastosFijosUseCase getGastosFijosUseCase, SaveGastoFijoUseCase saveGastoFijoUseCase, DeleteGastoFijoUseCase deleteGastoFijoUseCase) {
        this.getGastosFijosUseCase = getGastosFijosUseCase;
        this.saveGastoFijoUseCase = saveGastoFijoUseCase;
        this.deleteGastoFijoUseCase = deleteGastoFijoUseCase;
    }
}
