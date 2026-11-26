package com.vortise.gestion.controller;

import com.vortise.gestion.infrastructure.security.Guards;
import com.vortise.gestion.application.dto.EmpresaCreateDto;
import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.application.dto.EmpresaResumenDto;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.dto.PaginaDto;
import com.vortise.gestion.service.ProyectoService;
import com.vortise.gestion.service.EmpresaService;
import com.vortise.gestion.service.EmpresaConsultaService;
import com.vortise.gestion.service.RestriccionService;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.EmpresaDashboardObrasDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {
    private final ProyectoService proyectos;
    private final EmpresaService empresas;
    private final EmpresaConsultaService consulta;
    private final RestriccionService restricciones;

    public EmpresaController(EmpresaService empresas, ProyectoService proyectos,
            EmpresaConsultaService consulta, RestriccionService restricciones) {
        this.empresas = empresas;
        this.proyectos = proyectos;
        this.consulta = consulta;
        this.restricciones = restricciones;
    }

    @GetMapping
    public ResponseEntity<List<EmpresaDto>> getAll() {
        return ResponseEntity.ok(empresas.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(empresas.obtener(id));
    }

    @GetMapping("/{id}/obras/pagina")
    public ResponseEntity<PaginaDto<ProyectoDto>> getPagina(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(proyectos.paginar(id, q, page, size));
    }

    @GetMapping("/{id}/resumen")
    public ResponseEntity<EmpresaResumenDto> getResumen(@PathVariable Long id) {
        return ResponseEntity.ok(consulta.resumen(id));
    }

    @GetMapping("/{id}/dashboard-obras")
    public ResponseEntity<EmpresaDashboardObrasDto> getDashboardObras(@PathVariable Long id) {
        return ResponseEntity.ok(consulta.dashboardObras(id));
    }

    @GetMapping("/{id}/restricciones")
    public ResponseEntity<List<RestriccionDto>> getRestricciones(@PathVariable Long id) {
        return ResponseEntity.ok(restricciones.listarPorEmpresa(id));
    }

    @GetMapping("/{id}/obras")
    public ResponseEntity<List<ProyectoDto>> getObras(@PathVariable Long id) {
        return ResponseEntity.ok(proyectos.listarResumenesPorEmpresa(id));
    }

    @PostMapping
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<EmpresaDto> create(@Valid @RequestBody EmpresaCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresas.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<EmpresaDto> update(@PathVariable Long id, @Valid @RequestBody EmpresaCreateDto dto) {
        return ResponseEntity.ok(empresas.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        empresas.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
