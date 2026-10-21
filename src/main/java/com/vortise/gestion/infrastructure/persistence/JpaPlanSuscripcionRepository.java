package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.PlanSuscripcion;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPlanSuscripcionRepository extends JpaRepository<PlanSuscripcion, Long>, PlanSuscripcionRepository { }
