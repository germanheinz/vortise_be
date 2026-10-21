package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.TitularSuscripcion;

public record MeDto(Long usuarioId, String nombre, String email, String rol, Long empresaId, String empresaNombre,
                    boolean superAdmin, boolean suscripcionActiva, String planCodigo, TitularSuscripcion origenPlan,
                    boolean tieneFoto) { }
