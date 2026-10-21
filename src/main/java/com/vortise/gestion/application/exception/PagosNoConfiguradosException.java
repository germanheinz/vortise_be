package com.vortise.gestion.application.exception;

public class PagosNoConfiguradosException extends RuntimeException {
    public PagosNoConfiguradosException() { super("Pagos aún no configurados"); }
}
