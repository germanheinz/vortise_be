package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.RestriccionComentario;
import java.util.List;

public interface RestriccionComentarioRepository {
    List<RestriccionComentario> findByRestriccionIdOrderByCreadoEnAsc(Long restriccionId);

    RestriccionComentario save(RestriccionComentario comentario);
}