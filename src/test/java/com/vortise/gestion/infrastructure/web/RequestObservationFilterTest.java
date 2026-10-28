package com.vortise.gestion.infrastructure.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import static org.junit.jupiter.api.Assertions.*;

class RequestObservationFilterTest {
    @Test void generatesItsOwnIdLogsOnlyTemplateAndRestoresMdc() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/proyectos/987654");
        request.setQueryString("secret=synthetic-private-value");
        request.addHeader("X-Request-ID", "untrusted-client-id");
        request.addHeader("Authorization", "Bearer synthetic-secret");
        var response = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger(RequestObservationFilter.class);
        var capture = new ListAppender<ILoggingEvent>(); capture.start(); logger.addAppender(capture);
        MDC.put("requestId", "previous-context");
        try {
            new RequestObservationFilter().doFilter(request, response, (req, res) -> {
                assertEquals(response.getHeader("X-Request-ID"), MDC.get("requestId"));
                req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/proyectos/{id}");
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(403);
            });
            assertDoesNotThrow(() -> UUID.fromString(response.getHeader("X-Request-ID")));
            assertNotEquals("untrusted-client-id", response.getHeader("X-Request-ID"));
            var event = capture.list.get(0);
            Map<String, Object> fields = event.getKeyValuePairs().stream().collect(Collectors.toMap(k -> k.key, k -> k.value));
            assertEquals("/api/proyectos/{id}", fields.get("route"));
            assertEquals(403, fields.get("status"));
            assertEquals(response.getHeader("X-Request-ID"), fields.get("request_id"));
            assertFalse(fields.toString().contains("987654"));
            assertFalse(fields.toString().contains("synthetic-secret"));
            assertFalse(fields.toString().contains("synthetic-private-value"));
            assertNull(event.getThrowableProxy());
            assertEquals("previous-context", MDC.get("requestId"));
        } finally { MDC.clear(); logger.detachAppender(capture); capture.stop(); }
    }

    @Test void reportsUnhandledFailureWithoutLoggingItsMessageAndClearsMdc() throws Exception {
        var request = new MockHttpServletRequest("CUSTOM", "/private-resource");
        var response = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger(RequestObservationFilter.class);
        var capture = new ListAppender<ILoggingEvent>(); capture.start(); logger.addAppender(capture);
        try {
            assertThrows(IllegalStateException.class, () -> new RequestObservationFilter().doFilter(request, response,
                (req, res) -> { throw new IllegalStateException("synthetic-private-message"); }));
            var event = capture.list.get(0);
            Map<String, Object> fields = event.getKeyValuePairs().stream().collect(Collectors.toMap(k -> k.key, k -> k.value));
            assertEquals(500, fields.get("status")); assertEquals("OTHER", fields.get("method"));
            assertEquals("UNMATCHED", fields.get("route")); assertNull(event.getThrowableProxy());
            assertFalse(event.getFormattedMessage().contains("synthetic-private-message"));
            assertNull(MDC.get("requestId"));
        } finally { MDC.clear(); logger.detachAppender(capture); capture.stop(); }
    }

    @Test void fallbackLogsClassificationWithoutExceptionDetails() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var capture = new ListAppender<ILoggingEvent>(); capture.start(); logger.addAppender(capture);
        try {
            var response = new GlobalExceptionHandler().handleGeneral(new RuntimeException("synthetic-secret-payload"));
            assertEquals(500, response.getStatusCode().value());
            var event = capture.list.get(0);
            assertNull(event.getThrowableProxy());
            assertFalse(event.getFormattedMessage().contains("synthetic-secret-payload"));
            assertEquals("RuntimeException", event.getKeyValuePairs().get(0).value);
        } finally { logger.detachAppender(capture); capture.stop(); }
    }
}
