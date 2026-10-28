package com.vortise.gestion.application.port;

public interface PasswordMailGateway {
    boolean enabled();
    void sendReset(String email, String url);
    void sendChanged(String email);
}
