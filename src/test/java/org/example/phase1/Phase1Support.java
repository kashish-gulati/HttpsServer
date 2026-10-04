package org.example.phase1;

import org.example.harness.HttpResponse;
import org.example.harness.RawBytes;
import org.example.harness.ServerExtension.ServerUnderTest;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Shared checks for Phase 1: every response is HTTP/1.1, framed by Content-Length, and followed by a close. */
final class Phase1Support {
    private Phase1Support() {}

    static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(3);

    private static final Pattern IMF_FIXDATE = Pattern.compile(
            "(Mon|Tue|Wed|Thu|Fri|Sat|Sun), \\d{2} (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{4} \\d{2}:\\d{2}:\\d{2} GMT");

    /** Sends the bytes, half-closes, reads until the server closes, and checks the Phase 1 invariants. */
    static HttpResponse exchange(ServerUnderTest server, byte[] request) throws Exception {
        try (var conn = server.connect()) {
            conn.send(request);
            byte[] raw = conn.readUntilClose(RESPONSE_TIMEOUT);
            if (raw.length == 0) {
                fail("Server closed the connection without sending any response to:\n" + RawBytes.show(request));
            }
            HttpResponse response = HttpResponse.parse(raw);
            assertPhase1Invariants(response);
            return response;
        }
    }

    static HttpResponse exchange(ServerUnderTest server, String resourcePath) throws Exception {
        return exchange(server, RawBytes.resource(resourcePath));
    }

    static void assertPhase1Invariants(HttpResponse r) {
        String raw = "\n--- response ---\n" + RawBytes.show(r.raw());
        assertEquals("HTTP/1.1", r.version(), "Status line must carry the server's version, HTTP/1.1" + raw);
        assertTrue(r.header("Content-Length").isPresent(),
                "Every Phase 1 response (errors too) must have Content-Length" + raw);
        assertEquals("close", r.header("Connection").map(String::toLowerCase).orElse(null),
                "Phase 1 closes after every response and must say so with Connection: close" + raw);
        String date = r.header("Date").orElseThrow(() -> new AssertionError("Missing Date header (RFC 9110 §6.6.1)" + raw));
        try {
            ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (DateTimeParseException e) {
            fail("Date must be IMF-fixdate, e.g. 'Sat, 03 Oct 2026 16:04:10 GMT', got '" + date + "'" + raw);
        }
        assertTrue(IMF_FIXDATE.matcher(date).matches(),
                "Date must be IMF-fixdate exactly (2-digit day, GMT), e.g. 'Sat, 03 Oct 2026 16:04:10 GMT', got '"
                        + date + "' (RFC 9110 §5.6.7)" + raw);
    }
}
