package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.RestriccionComentario;
import com.vortise.gestion.domain.repository.RestriccionComentarioRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRestriccionComentarioRepository extends JpaRepository<RestriccionComentario, Long>, RestriccionComentarioRepository {
    @Override
    List<RestriccionComentario> findByRestriccionIdOrderByCreadoEnAsc(Long restriccionId);
}