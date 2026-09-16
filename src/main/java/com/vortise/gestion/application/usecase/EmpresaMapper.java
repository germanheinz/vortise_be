package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.domain.model.Empresa;

final class EmpresaMapper {
    private EmpresaMapper() {
    }

    static EmpresaDto toDto(Empresa empresa) {
        return new EmpresaDto(
            empresa.getId(),
            empresa.getNombre(),
            empresa.getIdentificacionFiscal(),
            empresa.getLimiteUsuarios(),
            empresa.getCreadoEn()
        );
    }
}
