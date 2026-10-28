package com.vortise.gestion.infrastructure.mail;

import com.vortise.gestion.application.port.PasswordMailGateway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpPasswordMailGateway implements PasswordMailGateway {
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;
    private final org.springframework.core.task.TaskExecutor executor;
    public SmtpPasswordMailGateway(ObjectProvider<JavaMailSender> sender,
            @Value("${app.password-mail.enabled:false}") boolean enabled,
            @Value("${app.password-mail.from:}") String from,
            @org.springframework.beans.factory.annotation.Qualifier("passwordMailExecutor") org.springframework.core.task.TaskExecutor executor) {
        this.sender = sender.getIfAvailable(); this.enabled = enabled; this.from = from;
        this.executor = executor;
        if (enabled && (this.sender == null || from.isBlank() || from.contains("\r") || from.contains("\n"))) {
            throw new IllegalStateException("Password email requires SMTP and a valid sender");
        }
    }
    public boolean enabled() { return enabled; }
    public void sendReset(String email, String url) {
        send(email, "Recuperar contraseña", "Recibimos una solicitud para cambiar tu contraseña. El enlace vence en 30 minutos y solo puede usarse una vez.\n\n" + url + "\n\nSi no hiciste esta solicitud, podés ignorar este correo.");
    }
    public void sendChanged(String email) {
        send(email, "Tu contraseña fue cambiada", "Se cambió la contraseña de tu cuenta en Gestión de Obras. Las sesiones anteriores ya no son válidas. Si no fuiste vos, recuperá tu contraseña y contactá al administrador.");
    }
    private void send(String email, String subject, String body) {
        if (!enabled) throw new IllegalStateException("Password email is disabled");
        var message = new SimpleMailMessage(); message.setFrom(from); message.setTo(email); message.setSubject(subject); message.setText(body);
        // Return the same public response without exposing SMTP timing for known accounts.
        executor.execute(() -> {
            try { sender.send(message); }
            catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(SmtpPasswordMailGateway.class).warn("Password security email delivery failed");
            }
        });
    }
}
