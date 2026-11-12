package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contract of a project. {@code subcontratistaId} null means own labour. When the contractor is created in the same
 * request, it is referenced by {@code subcontratistaNombre}; the name is read-only in responses.
 */
public record ContratoObraDto(Long id, @NotBlank @Size(max = 255) String nombre, Long subcontratistaId, String subcontratistaNombre) { }
