/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.application.dto;

import java.util.List;
import java.time.LocalDate;

public record ProyectoDto(
	Long id,
	String nombre,
	String descripcion,
	Double presupuestoUsd,
	String direccion,
	String numeroProyecto,
	LocalDate fechaInicio,
	LocalDate fechaFin,
	String responsablesObra,
	String categoriasManoObra,
	Integer cantidadSectores,
	String empresa,
	Long empresaId,
	String empresaNombre,
	Double horasPrevistas,
	Double horasReales,
	List<PlantaDto> plantas,
	List<RubroDto> rubros
) {
}
