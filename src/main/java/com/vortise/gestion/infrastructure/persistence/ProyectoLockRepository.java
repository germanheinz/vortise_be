package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Proyecto;
import java.util.Optional;

public interface ProyectoLockRepository { Optional<Proyecto> bloquearPorId(Long id); }
