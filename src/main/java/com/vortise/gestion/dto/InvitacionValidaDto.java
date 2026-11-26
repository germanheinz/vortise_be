package com.vortise.gestion.dto;

public record InvitacionValidaDto(
    String email,
    String nombre,
    String empresaNombre,
    boolean valida
) {}
