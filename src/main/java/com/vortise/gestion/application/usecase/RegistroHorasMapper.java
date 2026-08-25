package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RegistroHorasDto;
import com.vortise.gestion.domain.model.RegistroHoras;

final class RegistroHorasMapper {
    private RegistroHorasMapper() {
    }

    static RegistroHorasDto toDto(RegistroHoras registroHoras) {
        return new RegistroHorasDto(
            registroHoras.getId(),
            registroHoras.getProyecto() == null ? null : registroHoras.getProyecto().getId(),
            registroHoras.getProyecto() == null ? null : registroHoras.getProyecto().getNombre(),
            registroHoras.getNumeroRubro(),
            registroHoras.getRubro(),
            registroHoras.getSubRubro(),
            registroHoras.getResponsable(),
            registroHoras.getInicio(),
            registroHoras.getFin(),
            registroHoras.getHoras(),
            registroHoras.getDescripcion()
        );
    }
}