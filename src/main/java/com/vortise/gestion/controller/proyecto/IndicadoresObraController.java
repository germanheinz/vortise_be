package com.vortise.gestion.controller.proyecto;

import com.vortise.gestion.application.dto.IndicadoresObraDto;
import com.vortise.gestion.service.proyecto.IndicadoresObraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only indicators of a project; the tenant interceptor guards {@code /api/proyectos/{id}/**}. */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/indicadores")
public class IndicadoresObraController {
    private final IndicadoresObraService indicadores;

    public IndicadoresObraController(IndicadoresObraService indicadores) {
        this.indicadores = indicadores;
    }

    @GetMapping
    public IndicadoresObraDto obtener(@PathVariable Long proyectoId) {
        return indicadores.calcular(proyectoId);
    }
}
