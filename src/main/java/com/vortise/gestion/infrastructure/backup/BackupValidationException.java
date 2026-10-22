package com.vortise.gestion.infrastructure.backup;

public class BackupValidationException extends RuntimeException {
    public BackupValidationException(String message) {
        super(message);
    }

    public BackupValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
