package com.vortise.gestion.repository;

import com.vortise.gestion.model.RubroObra;
import java.util.Optional;

public interface RubroObraRepository {
    Optional<RubroObra> findById(Long id);
}