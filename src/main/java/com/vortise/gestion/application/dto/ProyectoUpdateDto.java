/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 */
package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
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
	List<Long> subcontratistaIds,
	Double superficieCubierta,
	Double superficieSemicubierta,
	Double superficieDescubierta,
	@Valid List<CategoriaManoObraCostoDto> costosManoObraModificados,
	@Valid List<SubcontratistaObraDto> contratistasObra
) {
	public ProyectoUpdateDto(String nombre, String descripcion, Double presupuestoUsd, String direccion, String numeroProyecto,
			LocalDate fechaInicio, LocalDate fechaFin, String responsablesObra, String categoriasManoObra, Integer cantidadSectores,
			Long empresaId, String empresa, Double horasPrevistas, Double horasReales, List<PlantaCreateDto> plantas,
			List<RubroCreateDto> rubros, List<Long> subcontratistaIds) {
		this(nombre, descripcion, presupuestoUsd, direccion, numeroProyecto, fechaInicio, fechaFin, responsablesObra,
			categoriasManoObra, cantidadSectores, empresaId, empresa, horasPrevistas, horasReales, plantas, rubros,
			subcontratistaIds, null, null, null, null, null);
	}
}
