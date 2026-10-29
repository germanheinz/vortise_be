package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RegistroHorasDatos;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RegistroHoras;
import java.time.Duration;

final class RegistroHorasRules {
    private RegistroHorasRules() { }
    static void apply(RegistroHorasDatos dto, RegistroHoras registroHoras, Proyecto proyecto) {
        if (dto.fin().isBefore(dto.inicio()) || dto.fin().isEqual(dto.inicio())) {
            throw new SolicitudInvalidaException("La hora de fin debe ser posterior al inicio");
        }

        registroHoras.setNumeroRubro(dto.numeroRubro());
        registroHoras.setRubro(dto.rubro());
        registroHoras.setSubRubro(dto.subRubro());
        registroHoras.setResponsable(dto.responsable());
        registroHoras.setEtapa(dto.etapa());
        registroHoras.setCantidadPrevista(dto.cantidadPrevista());
        registroHoras.setCantidadReal(dto.cantidadReal());
        registroHoras.setHorasPrevistas(dto.horasPrevistas());
        registroHoras.setHorasOficiales(dto.horasOficiales());
        registroHoras.setHorasAyudantes(dto.horasAyudantes());
        registroHoras.setCausaNoCumplimiento(dto.causaNoCumplimiento());
        registroHoras.setMedidaCorrectiva(dto.medidaCorrectiva());
        registroHoras.setInicio(dto.inicio());
        registroHoras.setFin(dto.fin());
        double horasCuadrilla = (dto.horasOficiales() == null ? 0 : dto.horasOficiales()) + (dto.horasAyudantes() == null ? 0 : dto.horasAyudantes());
        registroHoras.setHoras(horasCuadrilla > 0 ? roundHours(horasCuadrilla) : roundHours(Duration.between(dto.inicio(), dto.fin()).toMinutes() / 60.0));
        registroHoras.setProductividadPresupuesto(findProductividadPresupuesto(proyecto, dto.numeroRubro(), dto.rubro()));
        registroHoras.setDescripcion(dto.descripcion());
    }

    private static Double findProductividadPresupuesto(Proyecto proyecto, String numeroRubro, String rubro) {
        return proyecto.getRubros() == null ? null : proyecto.getRubros().stream()
            .filter(item -> numeroRubro == null || numeroRubro.equals(item.getnRubro()))
            .filter(item -> rubro == null || rubro.equalsIgnoreCase(item.getRubro()))
            .findFirst()
            .map(item -> item.getProductividad())
            .orElse(null);
    }

    static double roundHours(double hours) { return Math.round(hours * 100.0) / 100.0; }
}
