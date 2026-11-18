package com.vortise.gestion.application.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security audit trail on its own logger ("AUDIT"). Records who did what and the outcome, never emails,
 * passwords, tokens or request bodies.
 */
public final class AuditLog {
    private static final Logger LOG = LoggerFactory.getLogger("AUDIT");
    private static final int MAX_FIELD = 200;

    private AuditLog() { }

    public static void accessDenied(String code, Long actorUserId, String method, String path) {
        LOG.warn("event=access_denied code={} actor={} method={} path={}", clean(code), actorUserId, clean(method), clean(path));
    }

    public static void userAccessChanged(Long actorUserId, Long targetUserId, String role, int assignedSites, boolean active) {
        LOG.info("event=user_access_changed actor={} target={} role={} sites={} active={}",
            actorUserId, targetUserId, clean(role), assignedSites, active);
    }

    public static void userInvited(Long actorUserId, Long invitationId, String role, int assignedSites) {
        LOG.info("event=user_invited actor={} invitation={} role={} sites={}", actorUserId, invitationId, clean(role), assignedSites);
    }

    public static void userInvitationAccepted(Long userId, Long invitationId, String role) {
        LOG.info("event=user_invitation_accepted user={} invitation={} role={}", userId, invitationId, clean(role));
    }

    public static void refreshReuseDetected(Long userId) {
        LOG.warn("event=refresh_token_reuse actor={} action=family_revoked", userId);
    }

    // Strips line breaks and control characters so a request cannot forge log entries.
    static String clean(String value) {
        if (value == null) return "-";
        String cleaned = value.replaceAll("[\\p{Cntrl}]", "_");
        return cleaned.length() > MAX_FIELD ? cleaned.substring(0, MAX_FIELD) : cleaned;
    }
}
