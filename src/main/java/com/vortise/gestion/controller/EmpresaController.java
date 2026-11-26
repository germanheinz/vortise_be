package com.vortise.gestion.controller;

import com.vortise.gestion.application.dto.EmpresaCreateDto;
import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.application.dto.EmpresaResumenDto;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.dto.PaginaDto;
import com.vortise.gestion.application.service.EmpresaBajaService;
import com.vortise.gestion.application.service.EmpresaResumenService;
import com.vortise.gestion.application.service.EmpresaRestriccionesService;
import com.vortise.gestion.application.service.ProyectoConsultaService;
import com.vortise.gestion.application.service.EmpresaService;
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
    private final ProyectoConsultaService proyectos;
    private final EmpresaService empresas;
    private final EmpresaBajaService bajaEmpresa;
    private final EmpresaResumenService resumen;
    private final EmpresaRestriccionesService restriccionesEmpresa;
    private final com.vortise.gestion.application.service.EmpresaDashboardObrasService dashboardObras;

    public EmpresaController(EmpresaService empresas, EmpresaBajaService bajaEmpresa,
            ProyectoConsultaService proyectos,
            EmpresaResumenService resumen, EmpresaRestriccionesService restriccionesEmpresa,
            com.vortise.gestion.application.service.EmpresaDashboardObrasService dashboardObras) {
        this.dashboardObras = dashboardObras;
        this.proyectos = proyectos;
        this.resumen = resumen;
        this.restriccionesEmpresa = restriccionesEmpresa;
        this.empresas = empresas;
        this.bajaEmpresa = bajaEmpresa;
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
    public ResponseEntity<PaginaDto<com.vortise.gestion.application.dto.ProyectoDto>> getPagina(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(proyectos.paginar(id, q, page, size));
    }

    @GetMapping("/{id}/resumen")
    public ResponseEntity<EmpresaResumenDto> getResumen(@PathVariable Long id) {
        return ResponseEntity.ok(resumen.execute(id));
    }

    @GetMapping("/{id}/dashboard-obras")
    public ResponseEntity<com.vortise.gestion.application.dto.EmpresaDashboardObrasDto> getDashboardObras(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardObras.execute(id));
    }

    @GetMapping("/{id}/restricciones")
    public ResponseEntity<List<RestriccionDto>> getRestricciones(@PathVariable Long id) {
        return ResponseEntity.ok(restriccionesEmpresa.execute(id));
    }

    @GetMapping("/{id}/obras")
    public ResponseEntity<List<com.vortise.gestion.application.dto.ProyectoDto>> getObras(@PathVariable Long id) {
        return ResponseEntity.ok(proyectos.listarResumenesPorEmpresa(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EmpresaDto> create(@Valid @RequestBody EmpresaCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresas.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EmpresaDto> update(@PathVariable Long id, @Valid @RequestBody EmpresaCreateDto dto) {
        return ResponseEntity.ok(empresas.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bajaEmpresa.execute(id);
        return ResponseEntity.noContent().build();
    }
}
