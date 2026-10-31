package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SubcontratistaObraDto(Long id, @NotBlank String nombre) { }