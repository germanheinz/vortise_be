package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.vortise.gestion.infrastructure.mail.SmtpPasswordMailGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class PasswordMailGatewayTest {
    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        var p = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class); when(p.getIfAvailable()).thenReturn(sender); return p;
    }
    @Test void sendsLinkAndSecurityNoticeWithoutIncludingPassword() {
        var sender = mock(JavaMailSender.class);
        var mail = new SmtpPasswordMailGateway(provider(sender), true, "security@example.invalid", Runnable::run);
        mail.sendReset("test@example.invalid", "https://frontend.example.invalid/restablecer-password#token=synthetic");
        mail.sendChanged("test@example.invalid");
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class); verify(sender, times(2)).send(message.capture());
        assertTrue(message.getAllValues().get(0).getText().contains("#token=synthetic"));
        assertFalse(message.getAllValues().get(1).getText().contains("#token="));
    }
    @Test void disabledMailCannotSendAndEnabledMailRequiresConfiguration() {
        var mail = new SmtpPasswordMailGateway(provider(null), false, "", Runnable::run);
        assertFalse(mail.enabled()); assertThrows(IllegalStateException.class, () -> mail.sendChanged("test@example.invalid"));
        assertThrows(IllegalStateException.class, () -> new SmtpPasswordMailGateway(provider(null), true, "", Runnable::run));
    }
    @Test void providerFailureDoesNotExposeExceptionToRequestThread() {
        var sender = mock(JavaMailSender.class); doThrow(new IllegalStateException("private SMTP detail")).when(sender).send(any(SimpleMailMessage.class));
        var mail = new SmtpPasswordMailGateway(provider(sender), true, "security@example.invalid", Runnable::run);
        assertDoesNotThrow(() -> mail.sendChanged("test@example.invalid"));
    }
}
