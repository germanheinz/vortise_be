package com.vortise.gestion.application.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AuditLogTest {
    private final Logger audit = (Logger) LoggerFactory.getLogger("AUDIT");
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();

    @BeforeEach
    void attach() {
        events.start();
        audit.addAppender(events);
    }

    @AfterEach
    void detach() {
        audit.detachAppender(events);
    }

    @Test
    void deniedAccessIsRecordedAsAWarningWithTheActorAndRoute() {
        AuditLog.accessDenied("OBRA_NO_ASIGNADA", 12L, "PATCH", "/api/restricciones/9");

        assertEquals(1, events.list.size());
        assertEquals(Level.WARN, events.list.get(0).getLevel());
        assertEquals("event=access_denied code=OBRA_NO_ASIGNADA actor=12 method=PATCH path=/api/restricciones/9",
            events.list.get(0).getFormattedMessage());
    }

    @Test
    void userAccessChangesAreRecordedWithoutPersonalData() {
        AuditLog.userAccessChanged(1L, 2L, "JEFE_DE_OBRA", 3, true);

        assertEquals("event=user_access_changed actor=1 target=2 role=JEFE_DE_OBRA sites=3 active=true",
            events.list.get(0).getFormattedMessage());
    }

    @Test
    void requestValuesCannotForgeLogLines() {
        AuditLog.accessDenied("X", null, "POST", "/api/a\r\nevent=user_access_changed actor=1");

        String message = events.list.get(0).getFormattedMessage();
        assertFalse(message.contains("\n") || message.contains("\r"));
        assertTrue(message.contains("actor=null"));
    }

    @Test
    void veryLongValuesAreTruncated() {
        AuditLog.accessDenied("X", 1L, "POST", "/" + "a".repeat(1000));

        assertTrue(events.list.get(0).getFormattedMessage().length() < 400);
    }
}
