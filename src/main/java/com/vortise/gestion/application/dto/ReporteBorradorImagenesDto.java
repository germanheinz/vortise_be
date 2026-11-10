package com.vortise.gestion.application.dto;

import java.util.List;

/** Draft images for the next report plus the upload rules the screen must respect. */
public record ReporteBorradorImagenesDto(boolean subidaDisponible, int maximoImagenes, long maximoBytes, List<ReporteImagenDto> imagenes) { }
