package com.vortise.gestion.application.exception;

/** Password-based endpoints are switched off because credentials live in the identity provider; HTTP 410. */
public class LoginConContrasenaDesactivadoException extends RuntimeException {
    public static final String CODE = "LOGIN_CON_CONTRASENA_DESACTIVADO";

    public LoginConContrasenaDesactivadoException() {
        super("El acceso con contraseña de Vortise ya no está disponible. Ingresá con tu cuenta.");
    }
}
