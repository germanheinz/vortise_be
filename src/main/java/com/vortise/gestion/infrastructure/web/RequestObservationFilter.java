package com.vortise.gestion.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Synchronous servlet request metadata only. Never log URLs, payloads or principals. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestObservationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestObservationFilter.class);
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        String previous = MDC.get("requestId");
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        long start = System.nanoTime();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException failure) {
            failed = true;
            throw failure;
        } finally {
            try {
                Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                log.atInfo().addKeyValue("request_id", requestId)
                    .addKeyValue("method", METHODS.contains(request.getMethod()) ? request.getMethod() : "OTHER")
                    .addKeyValue("route", pattern == null ? "UNMATCHED" : pattern.toString())
                    .addKeyValue("status", failed ? 500 : response.getStatus())
                    .addKeyValue("duration_ms", (System.nanoTime() - start) / 1_000_000)
                    .log("http_request_completed");
            } finally {
                if (previous == null) MDC.remove("requestId"); else MDC.put("requestId", previous);
            }
        }
    }
}
