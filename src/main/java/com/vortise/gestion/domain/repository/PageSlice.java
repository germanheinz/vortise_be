package com.vortise.gestion.domain.repository;

import java.util.List;

/** Neutral bounded query result; Spring Data stays in the persistence adapter. */
public record PageSlice<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public PageSlice { content = List.copyOf(content); }
}
