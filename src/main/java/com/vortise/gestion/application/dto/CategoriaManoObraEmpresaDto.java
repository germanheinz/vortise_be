package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaManoObraEmpresaDto(Long id, @NotBlank String nombre, Double costoHora, boolean habilitada) { }
