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
            registroHoras.getEtapa(),
            registroHoras.getInicio(),
            registroHoras.getFin(),
            registroHoras.getHoras(),
            registroHoras.getDescripcion(),
            registroHoras.getCantidadPrevista(),
            registroHoras.getCantidadReal(),
            registroHoras.getHorasPrevistas(),
            registroHoras.getHorasOficiales(),
            registroHoras.getHorasAyudantes(),
            registroHoras.getProductividadPresupuesto(),
            calculateProductividadPrevista(registroHoras),
            calculateProductividadReal(registroHoras),
            calculateRendimiento(registroHoras),
            calculateCumplimiento(registroHoras),
            calculateEstado(registroHoras),
            registroHoras.getCausaNoCumplimiento(),
            registroHoras.getMedidaCorrectiva()
        );
    }

    private static Double calculateProductividadPrevista(RegistroHoras registro) {
        return divide(registro.getHorasPrevistas(), registro.getCantidadPrevista());
    }

    private static Double calculateProductividadReal(RegistroHoras registro) {
        return divide(registro.getHoras(), registro.getCantidadReal());
    }

    private static Double calculateRendimiento(RegistroHoras registro) {
        Double productividadReal = calculateProductividadReal(registro);
        Double productividadPrevista = calculateProductividadPrevista(registro);
        return divide(productividadReal, productividadPrevista);
    }

    private static Double calculateCumplimiento(RegistroHoras registro) {
        return registro.getCantidadPrevista() == null || registro.getCantidadPrevista() == 0 || registro.getCantidadReal() == null
            ? null
            : round(registro.getCantidadReal() / registro.getCantidadPrevista() * 100);
    }

    private static String calculateEstado(RegistroHoras registro) {
        Double rendimiento = calculateRendimiento(registro);
        if (rendimiento == null) return null;
        if (rendimiento < 1) return "ALTO";
        if (rendimiento > 1) return "BAJO";
        return "ESTABLE";
    }

    private static Double divide(Double numerator, Double denominator) {
        return numerator == null || denominator == null || denominator == 0 ? null : round(numerator / denominator);
    }

    private static Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}