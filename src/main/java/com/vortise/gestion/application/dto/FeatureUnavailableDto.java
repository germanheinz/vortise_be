package com.vortise.gestion.application.dto;

public record FeatureUnavailableDto(int status, String error, String code, String message) {
}
