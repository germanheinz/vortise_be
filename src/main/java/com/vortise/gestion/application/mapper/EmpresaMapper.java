package com.vortise.gestion.application.mapper;

import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.domain.model.Empresa;

public final class EmpresaMapper {
    private EmpresaMapper() {
    }

    public static EmpresaDto toDto(Empresa empresa) {
        return new EmpresaDto(
            empresa.getId(),
            empresa.getNombre(),
            empresa.getIdentificacionFiscal(),
            empresa.getLimiteUsuarios(),
            empresa.getCreadoEn()
        );
    }
}
