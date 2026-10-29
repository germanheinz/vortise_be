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
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.exception.PagosNoConfiguradosException;
import com.vortise.gestion.application.exception.ProveedorPagoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.infrastructure.backup.BackupValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ConflictoEstadoException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(ConflictoEstadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(value={IllegalArgumentException.class})
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto(400, "Bad Request", "La solicitud no es válida."));
    }

    @ExceptionHandler(value={IllegalStateException.class})
    public ResponseEntity<ErrorResponseDto> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CONFLICT).body(new ErrorResponseDto(409, "Conflict", "La operación no puede completarse en el estado actual."));
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

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,
        org.springframework.dao.OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponseDto> handlePersistenceConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto(409, "Conflict",
            "Los datos cambiaron o ya existe un registro equivalente. Actualizá la página y volvé a intentar."));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponseDto> handleMalformedInput(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto(400, "Bad Request", "El formato de la solicitud no es válido."));
    }

    @ExceptionHandler(value={Exception.class})
    public ResponseEntity<ErrorResponseDto> handleGeneral(Exception ex) {
        log.atError().addKeyValue("exception_type", ex.getClass().getSimpleName()).log("unexpected_server_error");
        return ResponseEntity.status((HttpStatusCode)HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDto(500, "Internal Server Error", "No pudimos completar la operación. Intentá nuevamente más tarde."));
    }
}
