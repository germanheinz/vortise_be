/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.application.dto;

import java.util.List;

public record ProyectoDto(
	Long id,
	String nombre,
	String descripcion,
	Double presupuestoUsd,
	String direccion,
	String numeroProyecto,
	String empresa,
	Double horasPrevistas,
	Double horasReales,
	List<PlantaDto> plantas,
	List<RubroDto> rubros
) {
}
