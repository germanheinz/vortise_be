package com.vortise.gestion.application.exception;

public class FeatureUnavailableException extends RuntimeException {
    public static final String CODE = "FEATURE_TEMPORARILY_UNAVAILABLE";

    public FeatureUnavailableException(String message) {
        super(message);
    }
}
