package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Empresa;
import java.util.List;
import java.util.Optional;

public interface EmpresaRepository {
    List<Empresa> findAll();

    Optional<Empresa> findById(Long id);

    Optional<Empresa> findByNombreIgnoreCase(String nombre);

    Optional<Empresa> findByIdentificacionFiscal(String identificacionFiscal);

    Empresa save(Empresa empresa);

    void deleteById(Long id);
}
