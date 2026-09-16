package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CatalogoEmpresaDto(Long id, @NotBlank String tipo, @NotBlank String codigo, @NotBlank String nombre) { }
