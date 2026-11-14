package com.vortise.gestion.application.dto;

import java.time.LocalDate;

public record ReporteImagenDto(Long id, String url, String leyenda, Integer ancho, Integer alto, Integer orden,
        LocalDate fecha, Long rubroId, String rubroNombre, Long nivelId, String nivelNombre) { }
