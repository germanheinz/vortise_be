package com.vortise.gestion.application.dto;

import java.util.List;

public record PaginaDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public PaginaDto { content = List.copyOf(content); }
}
