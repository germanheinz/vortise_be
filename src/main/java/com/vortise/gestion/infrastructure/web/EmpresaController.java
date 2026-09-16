package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.EmpresaCreateDto;
import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.application.usecase.DeleteEmpresaUseCase;
import com.vortise.gestion.application.usecase.GetEmpresaByIdUseCase;
import com.vortise.gestion.application.usecase.GetEmpresasUseCase;
import com.vortise.gestion.application.usecase.GetObrasByEmpresaUseCase;
import com.vortise.gestion.application.usecase.SaveEmpresaUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {
    private final GetEmpresasUseCase getEmpresasUseCase;
    private final GetEmpresaByIdUseCase getEmpresaByIdUseCase;
    private final SaveEmpresaUseCase saveEmpresaUseCase;
    private final DeleteEmpresaUseCase deleteEmpresaUseCase;
    private final GetObrasByEmpresaUseCase getObrasByEmpresaUseCase;

    public EmpresaController(GetEmpresasUseCase getEmpresasUseCase, GetEmpresaByIdUseCase getEmpresaByIdUseCase,
            SaveEmpresaUseCase saveEmpresaUseCase, DeleteEmpresaUseCase deleteEmpresaUseCase,
            GetObrasByEmpresaUseCase getObrasByEmpresaUseCase) {
        this.getEmpresasUseCase = getEmpresasUseCase;
        this.getEmpresaByIdUseCase = getEmpresaByIdUseCase;
        this.saveEmpresaUseCase = saveEmpresaUseCase;
        this.deleteEmpresaUseCase = deleteEmpresaUseCase;
        this.getObrasByEmpresaUseCase = getObrasByEmpresaUseCase;
    }

    @GetMapping
    public ResponseEntity<List<EmpresaDto>> getAll() {
        return ResponseEntity.ok(getEmpresasUseCase.execute());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getEmpresaByIdUseCase.execute(id));
    }

    @GetMapping("/{id}/obras")
    public ResponseEntity<List<com.vortise.gestion.application.dto.ProyectoDto>> getObras(@PathVariable Long id) {
        return ResponseEntity.ok(getObrasByEmpresaUseCase.execute(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EmpresaDto> create(@Valid @RequestBody EmpresaCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saveEmpresaUseCase.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EmpresaDto> update(@PathVariable Long id, @Valid @RequestBody EmpresaCreateDto dto) {
        return ResponseEntity.ok(saveEmpresaUseCase.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteEmpresaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}