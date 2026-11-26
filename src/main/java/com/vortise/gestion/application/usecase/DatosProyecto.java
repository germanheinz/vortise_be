package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PlantaCreateDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class DatosProyecto {
    private DatosProyecto() {
    }

    static void validarFechas(LocalDate inicio, LocalDate fin) {
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            throw new SolicitudInvalidaException("La fecha de fin debe ser posterior al inicio de la obra");
        }
    }

    static void validarSuperficies(Double... superficies) {
        for (Double superficie : superficies) {
            if (superficie != null && (!Double.isFinite(superficie) || superficie < 0)) {
                throw new SolicitudInvalidaException("Las superficies generales deben ser números no negativos");
            }
        }
    }

    static List<Planta> mapearPlantas(List<PlantaCreateDto> plantas, Proyecto proyecto) {
        if (plantas == null) {
            return new ArrayList<>();
        }
        List<Planta> resultado = new ArrayList<>();
        for (PlantaCreateDto plantaDto : plantas) {
            Planta planta = new Planta();
            planta.setProyecto(proyecto);
            planta.setNivel(plantaDto.nivel());
            planta.setSuperficie(plantaDto.superficie());
            planta.setSupCubierta(plantaDto.supCubierta());
            planta.setSupDescubierta(plantaDto.supDescubierta());
            planta.setSupCalculada(plantaDto.supCalculada());
            planta.setCoefPlantaTipo(plantaDto.coefPlantaTipo());
            resultado.add(planta);
        }
        return resultado;
    }
}
