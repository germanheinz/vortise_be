package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import com.vortise.gestion.infrastructure.stripe.StripeSdkGateway;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class StripeRefundParserTest {
    @Test
    void signedOlderSnapshotUsesRefundIdForCurrentStateLookup() throws Exception {
        String secret = "synthetic-test-signing-key";
        String body = "{\"id\":\"evt_test\",\"object\":\"event\",\"api_version\":\"2020-08-27\",\"type\":\"refund.updated\",\"data\":{\"object\":{\"id\":\"re_test\",\"object\":\"refund\",\"amount\":3000,\"status\":\"succeeded\"}}}";
        long timestamp = Instant.now().getEpochSecond();
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = java.util.HexFormat.of().formatHex(hmac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
        var event = new StripeSdkGateway("", secret).parsearWebhook(body, "t=" + timestamp + ",v1=" + signature);
        assertEquals("re_test", event.refundId());
        assertEquals("refund.updated", event.tipo());
        assertThrows(SolicitudInvalidaException.class, () -> new StripeSdkGateway("", secret).parsearWebhook(body, "invalid"));
    }

    @Test
    void clockCannotBeUsedWithoutASandboxKey() {
        assertThrows(IllegalArgumentException.class, () -> new StripeSdkGateway("", "", "clock_test"));
    }
}
