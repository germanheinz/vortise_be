package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.RubroObra;
import java.util.Optional;

public interface RubroObraRepository {
    Optional<RubroObra> findById(Long id);
}