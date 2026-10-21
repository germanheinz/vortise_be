package com.vortise.gestion.application.exception;

/** Payment provider failure; the message is safe to show, details stay in server logs. */
public class ProveedorPagoException extends RuntimeException {
    public ProveedorPagoException(Throwable cause) { super("No se pudo completar la operación con el proveedor de pagos", cause); }
}
