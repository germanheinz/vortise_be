/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 */
package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ProyectoCreateDto(
	@NotBlank String nombre,
	String descripcion,
	Double presupuestoUsd,
	@NotBlank String direccion,
	@NotBlank String numeroProyecto,
	@NotBlank String empresa,
	Double horasPrevistas,
	Double horasReales,
	List<PlantaCreateDto> plantas,
	List<RubroCreateDto> rubros
) {
}
