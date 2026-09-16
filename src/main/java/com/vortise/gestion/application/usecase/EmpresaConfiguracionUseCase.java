package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ResponsableEmpresaDto;
import com.vortise.gestion.application.dto.UnidadMetricaEmpresaDto;
import com.vortise.gestion.application.dto.CatalogoEmpresaDto;
import com.vortise.gestion.application.dto.CalendarioLaboralEmpresaDto;
import com.vortise.gestion.application.dto.CategoriaManoObraEmpresaDto;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.CalendarioLaboralEmpresa;
import com.vortise.gestion.domain.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.ResponsableEmpresa;
import com.vortise.gestion.domain.model.UnidadMetricaEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ResponsableEmpresaRepository;
import com.vortise.gestion.domain.repository.UnidadMetricaEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmpresaConfiguracionUseCase {
    private final EmpresaRepository empresaRepository;
    private final ResponsableEmpresaRepository responsableRepository;
    private final UnidadMetricaEmpresaRepository unidadRepository;
    private final CatalogoEmpresaRepository catalogoRepository;
    private final CalendarioLaboralEmpresaRepository calendarioRepository;
    private final CategoriaManoObraEmpresaRepository categoriaManoObraRepository;

    private static final List<Object[]> CATEGORIAS_POR_DEFECTO = List.of(
        new Object[]{"OFICIAL", true},
        new Object[]{"AYUDANTE", true},
        new Object[]{"OFICIAL_ESPECIALIZADO", false},
        new Object[]{"MEDIO_OFICIAL", false}
    );

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

    public List<ResponsableEmpresaDto> getResponsables(Long empresaId) {
        requireEmpresa(empresaId);
        return responsableRepository.findByEmpresaId(empresaId).stream().map(this::toDto).toList();
    }

    public ResponsableEmpresaDto saveResponsable(Long empresaId, ResponsableEmpresaDto dto) {
        Empresa empresa = requireEmpresa(empresaId);
        ResponsableEmpresa responsable = dto.id() == null ? new ResponsableEmpresa(empresa, dto.rol().trim(), dto.nombre().trim(), dto.email(), dto.telefono())
            : responsableRepository.findById(dto.id()).orElseThrow(() -> new IllegalArgumentException("Responsable no encontrado"));
        if (!responsable.getEmpresa().getId().equals(empresaId)) throw new IllegalArgumentException("El responsable no pertenece a la empresa");
        responsable.setRol(dto.rol().trim());
        responsable.setNombre(dto.nombre().trim());
        responsable.setEmail(normalize(dto.email()));
        responsable.setTelefono(normalize(dto.telefono()));
        return toDto(responsableRepository.save(responsable));
    }

    public void deleteResponsable(Long empresaId, Long id) {
        requireEmpresa(empresaId);
        ResponsableEmpresa responsable = responsableRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Responsable no encontrado"));
        if (!responsable.getEmpresa().getId().equals(empresaId)) throw new IllegalArgumentException("El responsable no pertenece a la empresa");
        responsableRepository.deleteById(id);
    }

    public List<UnidadMetricaEmpresaDto> getUnidades(Long empresaId) {
        Empresa empresa = requireEmpresa(empresaId);
        if (unidadRepository.findByEmpresaId(empresaId).isEmpty()) {
            List.of(new String[]{"ML", "Metro lineal"}, new String[]{"M2", "Metro cuadrado"}, new String[]{"M3", "Metro cubico"}, new String[]{"GLOBAL", "Global"}, new String[]{"MES", "Mes"})
                .forEach(item -> unidadRepository.save(new UnidadMetricaEmpresa(empresa, item[0], item[1])));
        }
        return unidadRepository.findByEmpresaId(empresaId).stream().map(this::toDto).toList();
    }

    public UnidadMetricaEmpresaDto saveUnidad(Long empresaId, UnidadMetricaEmpresaDto dto) {
        Empresa empresa = requireEmpresa(empresaId);
        UnidadMetricaEmpresa unidad = dto.id() == null ? new UnidadMetricaEmpresa(empresa, dto.codigo().trim(), dto.nombre().trim())
            : unidadRepository.findById(dto.id()).orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada"));
        if (!unidad.getEmpresa().getId().equals(empresaId)) throw new IllegalArgumentException("La unidad no pertenece a la empresa");
        unidad.setCodigo(dto.codigo().trim().toUpperCase());
        unidad.setNombre(dto.nombre().trim());
        return toDto(unidadRepository.save(unidad));
    }

    public void deleteUnidad(Long empresaId, Long id) {
        requireEmpresa(empresaId);
        UnidadMetricaEmpresa unidad = unidadRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada"));
        if (!unidad.getEmpresa().getId().equals(empresaId)) throw new IllegalArgumentException("La unidad no pertenece a la empresa");
        unidadRepository.deleteById(id);
    }

    public List<CatalogoEmpresaDto> getCatalogo(Long empresaId, String tipo) {
        requireEmpresa(empresaId);
        return catalogoRepository.findByEmpresaIdAndTipo(empresaId, tipo).stream().map(this::toDto).toList();
    }

    public CatalogoEmpresaDto saveCatalogo(Long empresaId, CatalogoEmpresaDto dto) {
        Empresa empresa = requireEmpresa(empresaId);
        CatalogoEmpresa item = dto.id() == null ? new CatalogoEmpresa(empresa, dto.tipo().trim().toUpperCase(), dto.codigo().trim(), dto.nombre().trim())
            : catalogoRepository.findById(dto.id()).orElseThrow(() -> new IllegalArgumentException("Catalogo no encontrado"));
        if (!item.getEmpresa().getId().equals(empresaId)) throw new IllegalArgumentException("El catalogo no pertenece a la empresa");
        item.setCodigo(dto.codigo().trim());
        item.setNombre(dto.nombre().trim());
        return toDto(catalogoRepository.save(item));
    }

    public void deleteCatalogo(Long empresaId, Long id) {
        requireEmpresa(empresaId);
        CatalogoEmpresa item = catalogoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Catalogo no encontrado"));
        if (!item.getEmpresa().getId().equals(empresaId)) throw new IllegalArgumentException("El catalogo no pertenece a la empresa");
        catalogoRepository.deleteById(id);
    }

    public CalendarioLaboralEmpresaDto getCalendario(Long empresaId) {
        Empresa empresa = requireEmpresa(empresaId);
        CalendarioLaboralEmpresa calendario = calendarioRepository.findByEmpresaId(empresaId).orElseGet(() -> calendarioRepository.save(new CalendarioLaboralEmpresa(empresa)));
        return toDto(calendario);
    }

    public CalendarioLaboralEmpresaDto saveCalendario(Long empresaId, CalendarioLaboralEmpresaDto dto) {
        requireEmpresa(empresaId);
        CalendarioLaboralEmpresa calendario = calendarioRepository.findByEmpresaId(empresaId).orElseGet(() -> new CalendarioLaboralEmpresa(requireEmpresa(empresaId)));
        calendario.setHorasLunesAViernes(dto.horasLunesAViernes());
        calendario.setHorasSabado(dto.horasSabado());
        calendario.setTrabajaDomingo(dto.trabajaDomingo());
        calendario.setPagaDobleFeriado(dto.pagaDobleFeriado());
        calendario.setPagaDobleSabado(dto.pagaDobleSabado());
        calendario.setPagaDobleNoLaborable(dto.pagaDobleNoLaborable());
        return toDto(calendarioRepository.save(calendario));
    }

    private Empresa requireEmpresa(Long id) {
        return empresaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }
    private String normalize(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private ResponsableEmpresaDto toDto(ResponsableEmpresa item) { return new ResponsableEmpresaDto(item.getId(), item.getRol(), item.getNombre(), item.getEmail(), item.getTelefono()); }
    private UnidadMetricaEmpresaDto toDto(UnidadMetricaEmpresa item) { return new UnidadMetricaEmpresaDto(item.getId(), item.getCodigo(), item.getNombre()); }
    private CatalogoEmpresaDto toDto(CatalogoEmpresa item) { return new CatalogoEmpresaDto(item.getId(), item.getTipo(), item.getCodigo(), item.getNombre()); }
    private CalendarioLaboralEmpresaDto toDto(CalendarioLaboralEmpresa item) { return new CalendarioLaboralEmpresaDto(item.getId(), item.getHorasLunesAViernes(), item.getHorasSabado(), item.isTrabajaDomingo(), item.isPagaDobleFeriado(), item.isPagaDobleSabado(), item.isPagaDobleNoLaborable()); }

    public List<CategoriaManoObraEmpresaDto> getCategoriasManoObra(Long empresaId) {
        Empresa empresa = requireEmpresa(empresaId);
        if (categoriaManoObraRepository.findByEmpresaId(empresaId).isEmpty()) {
            CATEGORIAS_POR_DEFECTO.forEach(item ->
                categoriaManoObraRepository.save(new CategoriaManoObraEmpresa(empresa, (String) item[0], null, (boolean) item[1])));
        }
        return categoriaManoObraRepository.findByEmpresaId(empresaId).stream().map(this::toDto).toList();
    }

    public CategoriaManoObraEmpresaDto saveCategoriaManoObra(Long empresaId, Long id, CategoriaManoObraEmpresaDto dto) {
        requireEmpresa(empresaId);
        CategoriaManoObraEmpresa categoria = categoriaManoObraRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        if (!categoria.getEmpresa().getId().equals(empresaId)) throw new IllegalArgumentException("La categoría no pertenece a la empresa");
        categoria.setCostoHora(dto.costoHora());
        categoria.setHabilitada(dto.habilitada());
        return toDto(categoriaManoObraRepository.save(categoria));
    }

    private CategoriaManoObraEmpresaDto toDto(CategoriaManoObraEmpresa item) {
        return new CategoriaManoObraEmpresaDto(item.getId(), item.getNombre(), item.getCostoHora(), item.isHabilitada());
    }
}
