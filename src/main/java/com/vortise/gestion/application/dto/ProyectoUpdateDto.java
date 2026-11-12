/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 */
package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
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
	boolean actualizarSuperficiesGenerales,
	@Valid List<CategoriaManoObraCostoDto> costosManoObraModificados,
	@Valid List<SubcontratistaObraDto> contratistasObra,
	@Min(0) Integer pedidosDeObraDias,
	boolean actualizarPedidosDeObraDias,
	@Valid List<ContratoObraDto> contratos
) {
	public ProyectoUpdateDto(String nombre, String descripcion, Double presupuestoUsd, String direccion, String numeroProyecto,
			LocalDate fechaInicio, LocalDate fechaFin, String responsablesObra, String categoriasManoObra, Integer cantidadSectores,
			Long empresaId, String empresa, Double horasPrevistas, Double horasReales, List<PlantaCreateDto> plantas,
			List<RubroCreateDto> rubros, List<Long> subcontratistaIds, Double superficieCubierta,
			Double superficieSemicubierta, Double superficieDescubierta, boolean actualizarSuperficiesGenerales,
			List<CategoriaManoObraCostoDto> costosManoObraModificados, List<SubcontratistaObraDto> contratistasObra,
			Integer pedidosDeObraDias, boolean actualizarPedidosDeObraDias) {
		this(nombre, descripcion, presupuestoUsd, direccion, numeroProyecto, fechaInicio, fechaFin, responsablesObra,
			categoriasManoObra, cantidadSectores, empresaId, empresa, horasPrevistas, horasReales, plantas, rubros,
			subcontratistaIds, superficieCubierta, superficieSemicubierta, superficieDescubierta,
			actualizarSuperficiesGenerales, costosManoObraModificados, contratistasObra, pedidosDeObraDias,
			actualizarPedidosDeObraDias, null);
	}

	public ProyectoUpdateDto(String nombre, String descripcion, Double presupuestoUsd, String direccion, String numeroProyecto,
			LocalDate fechaInicio, LocalDate fechaFin, String responsablesObra, String categoriasManoObra, Integer cantidadSectores,
			Long empresaId, String empresa, Double horasPrevistas, Double horasReales, List<PlantaCreateDto> plantas,
			List<RubroCreateDto> rubros, List<Long> subcontratistaIds, Double superficieCubierta,
			Double superficieSemicubierta, Double superficieDescubierta, boolean actualizarSuperficiesGenerales,
			List<CategoriaManoObraCostoDto> costosManoObraModificados, List<SubcontratistaObraDto> contratistasObra) {
		this(nombre, descripcion, presupuestoUsd, direccion, numeroProyecto, fechaInicio, fechaFin, responsablesObra,
			categoriasManoObra, cantidadSectores, empresaId, empresa, horasPrevistas, horasReales, plantas, rubros,
			subcontratistaIds, superficieCubierta, superficieSemicubierta, superficieDescubierta,
			actualizarSuperficiesGenerales, costosManoObraModificados, contratistasObra, null, false, null);
	}

	public ProyectoUpdateDto(String nombre, String descripcion, Double presupuestoUsd, String direccion, String numeroProyecto,
			LocalDate fechaInicio, LocalDate fechaFin, String responsablesObra, String categoriasManoObra, Integer cantidadSectores,
			Long empresaId, String empresa, Double horasPrevistas, Double horasReales, List<PlantaCreateDto> plantas,
			List<RubroCreateDto> rubros, List<Long> subcontratistaIds) {
		this(nombre, descripcion, presupuestoUsd, direccion, numeroProyecto, fechaInicio, fechaFin, responsablesObra,
			categoriasManoObra, cantidadSectores, empresaId, empresa, horasPrevistas, horasReales, plantas, rubros,
			subcontratistaIds, null, null, null, false, null, null, null, false, null);
	}
}
