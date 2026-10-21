package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.StripeEventoProcesado;
import com.vortise.gestion.domain.repository.StripeEventoProcesadoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaStripeEventoProcesadoRepository extends JpaRepository<StripeEventoProcesado, String>, StripeEventoProcesadoRepository { }
