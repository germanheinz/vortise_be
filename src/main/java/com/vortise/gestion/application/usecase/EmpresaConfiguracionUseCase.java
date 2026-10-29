package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CalendarioLaboralEmpresaDto;
import com.vortise.gestion.application.dto.CatalogoEmpresaDto;
import com.vortise.gestion.application.dto.CategoriaManoObraEmpresaDto;
import com.vortise.gestion.application.dto.ResponsableEmpresaDto;
import com.vortise.gestion.application.dto.UnidadMetricaEmpresaDto;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.CalendarioLaboralEmpresa;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.ResponsableEmpresa;
import com.vortise.gestion.domain.model.UnidadMetricaEmpresa;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ResponsableEmpresaRepository;
import com.vortise.gestion.domain.repository.UnidadMetricaEmpresaRepository;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaConfiguracionUseCase {
    private final EmpresaRepository empresaRepository;
    private final ResponsableEmpresaRepository responsableRepository;
    private final UnidadMetricaEmpresaRepository unidadRepository;
    private final CatalogoEmpresaRepository catalogoRepository;
    private final CalendarioLaboralEmpresaRepository calendarioRepository;
    private final CategoriaManoObraEmpresaRepository categoriaManoObraRepository;

    public EmpresaConfiguracionUseCase(EmpresaRepository empresaRepository, ResponsableEmpresaRepository responsableRepository,
            UnidadMetricaEmpresaRepository unidadRepository, CatalogoEmpresaRepository catalogoRepository,
            CalendarioLaboralEmpresaRepository calendarioRepository, CategoriaManoObraEmpresaRepository categoriaManoObraRepository) {
        this.empresaRepository = empresaRepository;
        this.responsableRepository = responsableRepository;
        this.unidadRepository = unidadRepository;
        this.catalogoRepository = catalogoRepository;
        this.calendarioRepository = calendarioRepository;
        this.categoriaManoObraRepository = categoriaManoObraRepository;
    }

    @Transactional(readOnly = true)
    public List<ResponsableEmpresaDto> getResponsables(Long empresaId) {
        requireEmpresa(empresaId);
        return responsableRepository.findByEmpresaId(empresaId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ResponsableEmpresaDto saveResponsable(Long empresaId, ResponsableEmpresaDto dto) {
        Empresa empresa = lockEmpresa(empresaId);
        ResponsableEmpresa responsable = dto.id() == null ? new ResponsableEmpresa(empresa, dto.rol().trim(), dto.nombre().trim(), dto.email(), dto.telefono())
            : responsableRepository.findById(dto.id()).orElseThrow(() -> new RecursoNoEncontradoException("Responsable no encontrado"));
        if (!responsable.getEmpresa().getId().equals(empresaId)) throw new SolicitudInvalidaException("El responsable no pertenece a la empresa");
        responsable.setRol(dto.rol().trim());
        responsable.setNombre(dto.nombre().trim());
        responsable.setEmail(normalize(dto.email()));
        responsable.setTelefono(normalize(dto.telefono()));
        return toDto(responsableRepository.save(responsable));
    }

    @Transactional
    public void deleteResponsable(Long empresaId, Long id) {
        lockEmpresa(empresaId);
        ResponsableEmpresa responsable = responsableRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Responsable no encontrado"));
        if (!responsable.getEmpresa().getId().equals(empresaId)) throw new SolicitudInvalidaException("El responsable no pertenece a la empresa");
        responsableRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UnidadMetricaEmpresaDto> getUnidades(Long empresaId) {
        requireEmpresa(empresaId);
        return unidadRepository.findByEmpresaId(empresaId).stream().map(this::toDto).toList();
    }

    @Transactional
    public UnidadMetricaEmpresaDto saveUnidad(Long empresaId, UnidadMetricaEmpresaDto dto) {
        Empresa empresa = lockEmpresa(empresaId);
        String nombre = dto.nombre().trim();
        UnidadMetricaEmpresa unidad = dto.id() == null ? new UnidadMetricaEmpresa(empresa, resolveUnidadCodigo(empresaId, dto.codigo(), nombre), nombre)
            : unidadRepository.findById(dto.id()).orElseThrow(() -> new RecursoNoEncontradoException("Unidad no encontrada"));
        if (!unidad.getEmpresa().getId().equals(empresaId)) throw new SolicitudInvalidaException("La unidad no pertenece a la empresa");
        if (dto.id() != null && dto.codigo() != null && !dto.codigo().isBlank()) unidad.setCodigo(dto.codigo().trim().toUpperCase(Locale.ROOT));
        unidad.setNombre(nombre);
        return toDto(unidadRepository.save(unidad));
    }

    private String resolveUnidadCodigo(Long empresaId, String requested, String nombre) {
        if (requested != null && !requested.isBlank()) return requested.trim().toUpperCase(Locale.ROOT);
        String base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "UNIDAD";
        base = base.substring(0, Math.min(base.length(), 40));
        Set<String> used = new HashSet<>();
        unidadRepository.findByEmpresaId(empresaId).forEach(item -> used.add(item.getCodigo().toUpperCase(Locale.ROOT)));
        String candidate = base;
        int suffix = 2;
        while (used.contains(candidate)) {
            String tail = "_" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 40 - tail.length())) + tail;
        }
        return candidate;
    }

    @Transactional
    public void deleteUnidad(Long empresaId, Long id) {
        lockEmpresa(empresaId);
        UnidadMetricaEmpresa unidad = unidadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Unidad no encontrada"));
        if (!unidad.getEmpresa().getId().equals(empresaId)) throw new SolicitudInvalidaException("La unidad no pertenece a la empresa");
        unidadRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CatalogoEmpresaDto> getCatalogo(Long empresaId, String tipo) {
        requireEmpresa(empresaId);
        return catalogoRepository.findByEmpresaIdAndTipo(empresaId, tipo).stream().map(this::toDto).toList();
    }

    @Transactional
    public CatalogoEmpresaDto saveCatalogo(Long empresaId, CatalogoEmpresaDto dto) {
        Empresa empresa = lockEmpresa(empresaId);
        CatalogoEmpresa item = dto.id() == null ? new CatalogoEmpresa(empresa, dto.tipo().trim().toUpperCase(), dto.codigo().trim(), dto.nombre().trim())
            : catalogoRepository.findById(dto.id()).orElseThrow(() -> new RecursoNoEncontradoException("Catalogo no encontrado"));
        if (!item.getEmpresa().getId().equals(empresaId)) throw new SolicitudInvalidaException("El catalogo no pertenece a la empresa");
        item.setCodigo(dto.codigo().trim());
        item.setNombre(dto.nombre().trim());
        return toDto(catalogoRepository.save(item));
    }

    @Transactional
    public void deleteCatalogo(Long empresaId, Long id) {
        lockEmpresa(empresaId);
        CatalogoEmpresa item = catalogoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Catalogo no encontrado"));
        if (!item.getEmpresa().getId().equals(empresaId)) throw new SolicitudInvalidaException("El catalogo no pertenece a la empresa");
        if ("SUBCONTRATISTA".equalsIgnoreCase(item.getTipo())
                && (catalogoRepository.existsInSelectedProjects(id) || catalogoRepository.existsAssignedToActivity(id))) {
            throw new ConflictoEstadoException("No se puede eliminar el subcontratista mientras esté seleccionado en una obra o asignado a un subrubro");
        }
        catalogoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CalendarioLaboralEmpresaDto getCalendario(Long empresaId) {
        requireEmpresa(empresaId);
        CalendarioLaboralEmpresa calendario = calendarioRepository.findByEmpresaId(empresaId).orElseThrow(() -> new RecursoNoEncontradoException("Calendario no configurado"));
        return toDto(calendario);
    }

    @Transactional
    public CalendarioLaboralEmpresaDto saveCalendario(Long empresaId, CalendarioLaboralEmpresaDto dto) {
        lockEmpresa(empresaId);
        CalendarioLaboralEmpresa calendario = calendarioRepository.findByEmpresaId(empresaId).orElseGet(() -> new CalendarioLaboralEmpresa(requireEmpresa(empresaId)));
        calendario.setHorasLunesAViernes(dto.horasLunesAViernes());
        calendario.setHorasSabado(dto.horasSabado());
        calendario.setTrabajaDomingo(dto.trabajaDomingo());
        calendario.setPagaDobleFeriado(dto.pagaDobleFeriado());
        calendario.setPagaDobleSabado(dto.pagaDobleSabado());
        calendario.setPagaDobleNoLaborable(dto.pagaDobleNoLaborable());
        return toDto(calendarioRepository.save(calendario));
    }

    private Empresa lockEmpresa(Long id) {
        return empresaRepository.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
    }
    private Empresa requireEmpresa(Long id) {
        return empresaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
    }
    private String normalize(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private ResponsableEmpresaDto toDto(ResponsableEmpresa item) { return new ResponsableEmpresaDto(item.getId(), item.getRol(), item.getNombre(), item.getEmail(), item.getTelefono()); }
    private UnidadMetricaEmpresaDto toDto(UnidadMetricaEmpresa item) { return new UnidadMetricaEmpresaDto(item.getId(), item.getCodigo(), item.getNombre()); }
    private CatalogoEmpresaDto toDto(CatalogoEmpresa item) { return new CatalogoEmpresaDto(item.getId(), item.getTipo(), item.getCodigo(), item.getNombre()); }
    private CalendarioLaboralEmpresaDto toDto(CalendarioLaboralEmpresa item) { return new CalendarioLaboralEmpresaDto(item.getId(), item.getHorasLunesAViernes(), item.getHorasSabado(), item.isTrabajaDomingo(), item.isPagaDobleFeriado(), item.isPagaDobleSabado(), item.isPagaDobleNoLaborable()); }

    @Transactional(readOnly = true)
    public List<CategoriaManoObraEmpresaDto> getCategoriasManoObra(Long empresaId) {
        requireEmpresa(empresaId);
        return categoriaManoObraRepository.findByEmpresaId(empresaId).stream().map(this::toDto).toList();
    }

    @Transactional
    public CategoriaManoObraEmpresaDto saveCategoriaManoObra(Long empresaId, Long id, CategoriaManoObraEmpresaDto dto) {
        lockEmpresa(empresaId);
        CategoriaManoObraEmpresa categoria = categoriaManoObraRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
        if (!categoria.getEmpresa().getId().equals(empresaId)) throw new SolicitudInvalidaException("La categoría no pertenece a la empresa");
        categoria.setCostoHora(dto.costoHora());
        categoria.setHabilitada(dto.habilitada());
        return toDto(categoriaManoObraRepository.save(categoria));
    }

    private CategoriaManoObraEmpresaDto toDto(CategoriaManoObraEmpresa item) {
        return new CategoriaManoObraEmpresaDto(item.getId(), item.getNombre(), item.getCostoHora(), item.isHabilitada());
    }
}
