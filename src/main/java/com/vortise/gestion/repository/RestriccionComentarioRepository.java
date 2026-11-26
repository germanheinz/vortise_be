package com.vortise.gestion.repository;

import com.vortise.gestion.model.RestriccionComentario;
import java.util.List;

public interface RestriccionComentarioRepository {
    List<RestriccionComentario> findByRestriccionIdOrderByCreadoEnAsc(Long restriccionId);

    RestriccionComentario save(RestriccionComentario comentario);
}