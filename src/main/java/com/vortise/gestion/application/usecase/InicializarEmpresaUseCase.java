package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Explicit idempotent command. Reads never create company configuration. */
@Service
public class InicializarEmpresaUseCase {
    private record Unidad(String codigo, String nombre) { }
    private record Categoria(String nombre, boolean habilitada) { }
    private static final List<Unidad> UNIDADES = List.of(new Unidad("ML", "Metro lineal"),
        new Unidad("M2", "Metro cuadrado"), new Unidad("M3", "Metro cubico"),
        new Unidad("GLOBAL", "Global"), new Unidad("MES", "Mes"));
    private static final List<Categoria> CATEGORIAS = List.of(new Categoria("OFICIAL", true),
        new Categoria("AYUDANTE", true), new Categoria("OFICIAL_ESPECIALIZADO", false),
        new Categoria("MEDIO_OFICIAL", false));
    private final EmpresaRepository empresas;
    private final UnidadMetricaEmpresaRepository unidades;
    private final CategoriaManoObraEmpresaRepository categorias;
    private final CalendarioLaboralEmpresaRepository calendarios;

    public InicializarEmpresaUseCase(EmpresaRepository empresas, UnidadMetricaEmpresaRepository unidades,
            CategoriaManoObraEmpresaRepository categorias, CalendarioLaboralEmpresaRepository calendarios) {
        this.empresas = empresas; this.unidades = unidades; this.categorias = categorias; this.calendarios = calendarios;
    }

    @Transactional
    public void execute(Long empresaId) {
        Empresa empresa = empresas.bloquearPorId(empresaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
        if (empresa.isConfiguracionInicializada()) return;
        Set<String> existentes = unidades.findByEmpresaId(empresaId).stream()
            .map(UnidadMetricaEmpresa::getCodigo).collect(Collectors.toSet());
        UNIDADES.stream().filter(u -> !existentes.contains(u.codigo()))
            .forEach(u -> unidades.save(new UnidadMetricaEmpresa(empresa, u.codigo(), u.nombre())));
        Set<String> nombres = categorias.findByEmpresaId(empresaId).stream()
            .map(CategoriaManoObraEmpresa::getNombre).collect(Collectors.toSet());
        CATEGORIAS.stream().filter(c -> !nombres.contains(c.nombre()))
            .forEach(c -> categorias.save(new CategoriaManoObraEmpresa(empresa, c.nombre(), null, c.habilitada())));
        if (calendarios.findByEmpresaId(empresaId).isEmpty()) {
            calendarios.save(new CalendarioLaboralEmpresa(empresa));
        }
        empresa.marcarConfiguracionInicializada();
        empresas.save(empresa);
    }
}
