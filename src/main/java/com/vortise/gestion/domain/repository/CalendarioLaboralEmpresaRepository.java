package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.CalendarioLaboralEmpresa;
import java.util.Optional;

public interface CalendarioLaboralEmpresaRepository {
    Optional<CalendarioLaboralEmpresa> findByEmpresaId(Long empresaId);
    CalendarioLaboralEmpresa save(CalendarioLaboralEmpresa calendario);
    void deleteById(Long id);
}
