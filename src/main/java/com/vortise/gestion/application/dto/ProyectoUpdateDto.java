/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 */
package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

public record ProyectoUpdateDto(
	@NotBlank String nombre,
	String descripcion,
	Double presupuestoUsd,
	@NotBlank String direccion,
	@NotBlank String numeroProyecto,
	LocalDate fechaInicio,
	LocalDate fechaFin,
	String responsablesObra,
	String categoriasManoObra,
	Integer cantidadSectores,
	Long empresaId,
	String empresa,
	Double horasPrevistas,
	Double horasReales,
	List<PlantaCreateDto> plantas,
	List<RubroCreateDto> rubros,
	List<Long> subcontratistaIds
) {
}
