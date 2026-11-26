package com.vortise.gestion.repository;

import com.vortise.gestion.model.ResponsableEmpresa;
import java.util.List;
import java.util.Optional;

public interface ResponsableEmpresaRepository {
    List<ResponsableEmpresa> findByEmpresaId(Long empresaId);
    Optional<ResponsableEmpresa> findById(Long id);
    ResponsableEmpresa save(ResponsableEmpresa responsable);
    void deleteById(Long id);
}
