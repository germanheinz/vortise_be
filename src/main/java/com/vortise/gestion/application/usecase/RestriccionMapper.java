package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionComentarioDto;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.domain.model.RestriccionComentario;
import com.vortise.gestion.domain.model.RestriccionObra;

final class RestriccionMapper {
    private RestriccionMapper() {
    }

    static RestriccionDto toDto(RestriccionObra restriccion) {
        return new RestriccionDto(
            restriccion.getId(),
            restriccion.getProyecto().getId(),
            restriccion.getProyecto().getNombre(),
            restriccion.getRestriccion(),
            restriccion.getTipo(),
            restriccion.getSector(),
            restriccion.getNivel(),
            restriccion.getUnidad(),
            restriccion.getRubro(),
            restriccion.getResponsable(),
            restriccion.getFechaSolicitud(),
            restriccion.getFechaMaximaEntrega(),
            restriccion.getFechaRealEntrega(),
            restriccion.getCarga(),
            restriccion.getPrioridad(),
            restriccion.getEstado(),
            restriccion.getOrden()
        );
    }

    static RestriccionComentarioDto toComentarioDto(RestriccionComentario comentario) {
        return new RestriccionComentarioDto(
            comentario.getId(),
            comentario.getRestriccion().getId(),
            comentario.getAutor(),
            comentario.getMensaje(),
            comentario.getCreadoEn()
        );
    }
}