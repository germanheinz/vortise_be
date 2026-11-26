package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.RubroObra;
import com.vortise.gestion.repository.RubroObraRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRubroObraRepository extends JpaRepository<RubroObra, Long>, RubroObraRepository { }