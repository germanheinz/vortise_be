package com.vortise.gestion.application.dto;

public record InvitacionValidaDto(
    String email,
    String nombre,
    String empresaNombre,
    boolean valida
) {}
