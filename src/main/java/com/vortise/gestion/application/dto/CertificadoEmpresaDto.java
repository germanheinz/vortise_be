package com.vortise.gestion.application.dto;

/** Company with activities in the obra; {@code subcontratistaId} is null for the own labor of the obra's company. */
public record CertificadoEmpresaDto(Long subcontratistaId, String nombre, boolean propia, int actividades, int certificados, boolean tieneAbierto) { }
