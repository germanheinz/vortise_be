package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.Size;

public record ReporteImagenLeyendaDto(@Size(max = 255) String leyenda) { }
