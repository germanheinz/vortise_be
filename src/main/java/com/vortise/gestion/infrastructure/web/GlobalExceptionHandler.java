/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.MethodArgumentNotValidException
 *  org.springframework.web.bind.annotation.ExceptionHandler
 *  org.springframework.web.bind.annotation.RestControllerAdvice
 */
package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.ErrorResponseDto;
import com.vortise.gestion.application.dto.FeatureUnavailableDto;
import com.vortise.gestion.application.exception.PagosNoConfiguradosException;
import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.exception.ProveedorPagoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.infrastructure.backup.BackupValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto(401, "Unauthorized", "Email o contraseña incorrectos"));
    }
    @ExceptionHandler(value={BackupValidationException.class})
    public ResponseEntity<ErrorResponseDto> handleBackupValidation(BackupValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDto(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(value={IllegalArgumentException.class})
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.NOT_FOUND).body(new ErrorResponseDto(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(value={IllegalStateException.class})
    public ResponseEntity<ErrorResponseDto> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CONFLICT).body(new ErrorResponseDto(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(value={AccessDeniedException.class})
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.FORBIDDEN).body(new ErrorResponseDto(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream().map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).findFirst().orElse("Validation error");
        return ResponseEntity.status((HttpStatusCode)HttpStatus.BAD_REQUEST).body(new ErrorResponseDto(400, "Bad Request", msg));
    }

    @ExceptionHandler(value={SolicitudInvalidaException.class})
    public ResponseEntity<ErrorResponseDto> handleSolicitudInvalida(SolicitudInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDto(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(value={PagosNoConfiguradosException.class})
    public ResponseEntity<ErrorResponseDto> handlePagosNoConfigurados(PagosNoConfiguradosException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponseDto(503, "Service Unavailable", ex.getMessage()));
    }

    @ExceptionHandler(value={FeatureUnavailableException.class})
    public ResponseEntity<FeatureUnavailableDto> handleFeatureUnavailable(FeatureUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new FeatureUnavailableDto(503, "Service Unavailable", FeatureUnavailableException.CODE, ex.getMessage()));
    }

    @ExceptionHandler(value={ProveedorPagoException.class})
    public ResponseEntity<ErrorResponseDto> handleProveedorPago(ProveedorPagoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponseDto(502, "Bad Gateway", ex.getMessage()));
    }

    @ExceptionHandler(value={MaxUploadSizeExceededException.class})
    public ResponseEntity<ErrorResponseDto> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ErrorResponseDto(413, "Payload Too Large", "El archivo es demasiado grande"));
    }

    @ExceptionHandler(value={Exception.class})
    public ResponseEntity<ErrorResponseDto> handleGeneral(Exception ex) {
        log.atError().addKeyValue("exception_type", ex.getClass().getSimpleName()).log("unexpected_server_error");
        return ResponseEntity.status((HttpStatusCode)HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDto(500, "Internal Server Error", "No pudimos completar la operación. Intentá nuevamente más tarde."));
    }
}
