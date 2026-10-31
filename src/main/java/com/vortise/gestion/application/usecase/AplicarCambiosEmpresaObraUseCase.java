package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CategoriaManoObraCostoDto;
import com.vortise.gestion.application.dto.SubcontratistaObraDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AplicarCambiosEmpresaObraUseCase {
    private static final String SUBCONTRATISTA = "SUBCONTRATISTA";
    private final CurrentIdentity identity;
    private final EmpresaRepository empresas;
    private final CatalogoEmpresaRepository catalogos;
    private final CategoriaManoObraEmpresaRepository categorias;

    public AplicarCambiosEmpresaObraUseCase(CurrentIdentity identity, EmpresaRepository empresas,
            CatalogoEmpresaRepository catalogos, CategoriaManoObraEmpresaRepository categorias) {
        this.identity = identity;
        this.empresas = empresas;
        this.catalogos = catalogos;
        this.categorias = categorias;
    }

    @Transactional
    public List<Long> execute(Long empresaId, List<CategoriaManoObraCostoDto> costos,
            List<SubcontratistaObraDto> contratistas) {
        List<CategoriaManoObraCostoDto> actualizacionesCosto = costos == null ? List.of() : costos;
        List<SubcontratistaObraDto> cambiosContratistas = contratistas == null ? List.of() : contratistas;
        if (actualizacionesCosto.isEmpty() && cambiosContratistas.isEmpty()) return List.of();
        identity.requireEmpresa(empresaId);
        if (!identity.isAdministrator()) throw new AccessDeniedException("Se requiere administración de la empresa para modificar sus catálogos");
        Empresa empresa = empresas.bloquearPorId(empresaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
        actualizarCostos(empresaId, actualizacionesCosto);
        return actualizarContratistas(empresa, cambiosContratistas);
    }

    private void actualizarCostos(Long empresaId, List<CategoriaManoObraCostoDto> cambios) {
        Set<Long> ids = new HashSet<>();
        for (CategoriaManoObraCostoDto cambio : cambios) {
            if (cambio == null || cambio.id() == null) throw new SolicitudInvalidaException("La categoría de mano de obra es obligatoria");
            if (!ids.add(cambio.id())) throw new SolicitudInvalidaException("Hay categorías de mano de obra duplicadas");
            Double costo = cambio.costoHora();
            if (costo != null && (!Double.isFinite(costo) || costo < 0)) {
                throw new SolicitudInvalidaException("El valor hora debe ser un número no negativo");
            }
            CategoriaManoObraEmpresa categoria = categorias.findById(cambio.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría de mano de obra no encontrada"));
            if (!empresaId.equals(categoria.getEmpresa().getId())) {
                throw new SolicitudInvalidaException("La categoría de mano de obra no pertenece a la empresa");
            }
            categoria.setCostoHora(costo);
            categorias.save(categoria);
        }
    }

    private List<Long> actualizarContratistas(Empresa empresa, List<SubcontratistaObraDto> cambios) {
        Long empresaId = empresa.getId();
        List<CatalogoEmpresa> existentes = catalogos.findByEmpresaIdAndTipo(empresaId, SUBCONTRATISTA);
        Set<Long> idsProcesados = new HashSet<>();
        Set<String> nombresProcesados = new HashSet<>();
        List<Long> ids = new ArrayList<>();
        for (SubcontratistaObraDto cambio : cambios) {
            if (cambio == null || cambio.nombre() == null) throw new SolicitudInvalidaException("El nombre del contratista es obligatorio");
            String nombre = cambio.nombre().trim();
            String clave = nombre.toLowerCase(Locale.ROOT);
            if (nombre.isEmpty() || !nombresProcesados.add(clave)) {
                throw new SolicitudInvalidaException("Los nombres de contratista deben ser válidos y únicos");
            }
            CatalogoEmpresa contratista;
            if (cambio.id() == null) {
                contratista = existentes.stream().filter(item -> item.getNombre().trim().equalsIgnoreCase(nombre)).findFirst()
                    .orElseGet(() -> catalogos.save(new CatalogoEmpresa(empresa, SUBCONTRATISTA, nombre.toUpperCase(Locale.ROOT), nombre)));
            } else {
                if (!idsProcesados.add(cambio.id())) throw new SolicitudInvalidaException("Hay contratistas duplicados");
                contratista = catalogos.findById(cambio.id())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Contratista no encontrado"));
                if (!Objects.equals(empresaId, contratista.getEmpresa().getId()) || !SUBCONTRATISTA.equalsIgnoreCase(contratista.getTipo())) {
                    throw new SolicitudInvalidaException("El contratista no pertenece a la empresa");
                }
                Long contratistaId = contratista.getId();
                boolean nombreOcupado = existentes.stream().anyMatch(item -> !item.getId().equals(contratistaId)
                    && item.getNombre().trim().equalsIgnoreCase(nombre));
                if (nombreOcupado) throw new SolicitudInvalidaException("Ya existe un contratista con ese nombre en la empresa");
                contratista.setNombre(nombre);
                contratista = catalogos.save(contratista);
            }
            if (!ids.contains(contratista.getId())) ids.add(contratista.getId());
        }
        return ids;
    }
}