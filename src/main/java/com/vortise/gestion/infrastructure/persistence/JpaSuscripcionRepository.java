package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.Suscripcion;
import com.vortise.gestion.repository.SuscripcionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaSuscripcionRepository extends JpaRepository<Suscripcion, Long>, SuscripcionRepository { }
