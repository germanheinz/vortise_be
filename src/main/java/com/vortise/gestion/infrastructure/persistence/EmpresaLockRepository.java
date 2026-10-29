package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Empresa;
import java.util.Optional;

public interface EmpresaLockRepository { Optional<Empresa> bloquearPorId(Long id); }
