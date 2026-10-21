package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.PagoSuscripcion;
import com.vortise.gestion.domain.repository.PagoSuscripcionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPagoSuscripcionRepository extends JpaRepository<PagoSuscripcion, Long>, PagoSuscripcionRepository { }
