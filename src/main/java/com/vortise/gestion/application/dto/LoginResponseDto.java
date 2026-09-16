package com.vortise.gestion.application.dto;

public record LoginResponseDto(String token, Long usuarioId, Long empresaId, String empresaNombre, String nombre, String email, String rol) { }
