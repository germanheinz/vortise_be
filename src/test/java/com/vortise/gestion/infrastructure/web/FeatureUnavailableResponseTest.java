package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class FeatureUnavailableResponseTest {
    @Test
    void usesStableCodeAndServiceUnavailableStatus() {
        var response = new GlobalExceptionHandler().handleFeatureUnavailable(
            new FeatureUnavailableException("Temporalmente deshabilitado"));
        var body = response.getBody();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(body);
        assertEquals("FEATURE_TEMPORARILY_UNAVAILABLE", body.code());
        assertEquals("Temporalmente deshabilitado", body.message());
    }
}
