package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.ResponsableEmpresaRepository;
import com.vortise.gestion.domain.repository.UnidadMetricaEmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DeleteEmpresaUseCase {
    private final EmpresaRepository empresaRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final ResponsableEmpresaRepository responsableEmpresaRepository;
    private final UnidadMetricaEmpresaRepository unidadMetricaEmpresaRepository;
    private final CatalogoEmpresaRepository catalogoEmpresaRepository;
    private final CategoriaManoObraEmpresaRepository categoriaManoObraEmpresaRepository;
    private final CalendarioLaboralEmpresaRepository calendarioLaboralEmpresaRepository;

    public DeleteEmpresaUseCase(EmpresaRepository empresaRepository, ProyectoRepository proyectoRepository,
            UsuarioEmpresaRepository usuarioEmpresaRepository, ResponsableEmpresaRepository responsableEmpresaRepository,
            UnidadMetricaEmpresaRepository unidadMetricaEmpresaRepository, CatalogoEmpresaRepository catalogoEmpresaRepository,
            CategoriaManoObraEmpresaRepository categoriaManoObraEmpresaRepository,
            CalendarioLaboralEmpresaRepository calendarioLaboralEmpresaRepository) {
        this.empresaRepository = empresaRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.responsableEmpresaRepository = responsableEmpresaRepository;
        this.unidadMetricaEmpresaRepository = unidadMetricaEmpresaRepository;
        this.catalogoEmpresaRepository = catalogoEmpresaRepository;
        this.categoriaManoObraEmpresaRepository = categoriaManoObraEmpresaRepository;
        this.calendarioLaboralEmpresaRepository = calendarioLaboralEmpresaRepository;
    }

    @Transactional
    public void execute(Long id) {
        if (empresaRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Empresa no encontrada");
        }
        if (proyectoRepository.countByEmpresaRelacionadaId(id) > 0) {
            throw new IllegalStateException("No se puede eliminar una empresa que tiene obras asociadas");
        }

        usuarioEmpresaRepository.findByEmpresaId(id).forEach(usuario -> usuarioEmpresaRepository.deleteById(usuario.getId()));
        responsableEmpresaRepository.findByEmpresaId(id).forEach(item -> responsableEmpresaRepository.deleteById(item.getId()));
        unidadMetricaEmpresaRepository.findByEmpresaId(id).forEach(item -> unidadMetricaEmpresaRepository.deleteById(item.getId()));
        catalogoEmpresaRepository.findByEmpresaId(id).forEach(item -> catalogoEmpresaRepository.deleteById(item.getId()));
        categoriaManoObraEmpresaRepository.findByEmpresaId(id).forEach(item -> categoriaManoObraEmpresaRepository.deleteById(item.getId()));
        calendarioLaboralEmpresaRepository.findByEmpresaId(id).ifPresent(item -> calendarioLaboralEmpresaRepository.deleteById(item.getId()));

        empresaRepository.deleteById(id);
    }
}
