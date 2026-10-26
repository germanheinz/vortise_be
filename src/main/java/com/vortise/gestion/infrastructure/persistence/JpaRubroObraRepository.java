package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRubroObraRepository extends JpaRepository<RubroObra, Long>, RubroObraRepository { }