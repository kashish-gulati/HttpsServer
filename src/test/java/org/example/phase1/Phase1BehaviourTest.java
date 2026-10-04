package org.example.phase1;

import org.example.harness.HttpResponse;
import org.example.harness.RawBytes;
import org.example.harness.ServerExtension;
import org.example.harness.ServerExtension.ServerUnderTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.example.phase1.Phase1Support.RESPONSE_TIMEOUT;
import static org.example.phase1.Phase1Support.exchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Behaviour that a single request file can't express: timing, fragmentation, HEAD, and surviving bad clients. */
@ExtendWith(ServerExtension.class)
class Phase1BehaviourTest {

    private static final byte[] GET = RawBytes.fromEscaped("GET / HTTP/1.1\\r\\nHost: localhost\\r\\n\\r\\n");

    @Test
    void getRootReturnsBodyMatchingContentLength(ServerUnderTest server) throws Exception {
        HttpResponse r = exchange(server, GET);
        assertEquals(200, r.status());
        assertTrue(r.body().length > 0, "GET / should return a non-empty body");
        // HttpResponse.parse already failed the test if Content-Length != actual body length.
    }

    @Test
    void reasonPhraseMatchesStatus(ServerUnderTest server) throws Exception {
        assertEquals("OK", exchange(server, GET).reason());
        assertEquals("Not Found", exchange(server, "requests/phase1/good/not-found.req").reason());
        assertEquals("Bad Request", exchange(server, "requests/phase1/bad/missing-host.req").reason());
    }

    @Test
    void dateHeaderIsCurrent(ServerUnderTest server) throws Exception {
        String date = exchange(server, GET).header("Date").orElseThrow();
        ZonedDateTime sent = ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME);
        Duration skew = Duration.between(sent, ZonedDateTime.now(ZoneOffset.UTC)).abs();
        assertTrue(skew.toSeconds() < 5, "Date '" + date + "' is " + skew.toSeconds() + "s off; is it really GMT?");
    }

    @Test
    void headHasSameHeadersAsGetButNoBody(ServerUnderTest server) throws Exception {
        HttpResponse get = exchange(server, GET);
        byte[] raw;
        try (var conn = server.connect()) {
            conn.send("HEAD / HTTP/1.1\\r\\nHost: localhost\\r\\n\\r\\n");
            raw = conn.readUntilClose(RESPONSE_TIMEOUT);
        }
        HttpResponse head = HttpResponse.parseHeadResponse(raw);
        Phase1Support.assertPhase1Invariants(head);
        assertEquals(200, head.status());
        assertEquals(get.header("Content-Length"), head.header("Content-Length"),
                "HEAD must report the Content-Length that GET would send (RFC 9110 §9.3.2)");
    }

    @Test
    void requestArrivingOneByteAtATime(ServerUnderTest server) throws Exception {
        try (var conn = server.connect()) {
            conn.sendInChunks(GET, 1, Duration.ofMillis(5));
            HttpResponse r = HttpResponse.parse(conn.readUntilClose(RESPONSE_TIMEOUT));
            assertEquals(200, r.status(), "A request split into 1-byte reads must still parse");
        }
    }

    @Test
    void terminatorSplitAcrossReads(ServerUnderTest server) throws Exception {
        // "...\r\n\r" then, later, "\n": the end-of-head marker straddles two read() calls.
        try (var conn = server.connect()) {
            conn.send(java.util.Arrays.copyOf(GET, GET.length - 1));
            Thread.sleep(200);
            conn.send(new byte[] {'\n'});
            assertEquals(200, HttpResponse.parse(conn.readUntilClose(RESPONSE_TIMEOUT)).status());
        }
    }

    @Test
    void doesNotRespondBeforeHeadIsComplete(ServerUnderTest server) throws Exception {
        try (var conn = server.connect()) {
            conn.send("GET / HTTP/1.1\\r\\nHost: localhost\\r\\n"); // missing the final empty line
            assertTrue(!conn.serverClosed(Duration.ofMillis(500)),
                    "Server answered or closed before the request head was complete");
            conn.send("\\r\\n");
            assertEquals(200, HttpResponse.parse(conn.readUntilClose(RESPONSE_TIMEOUT)).status());
        }
    }

    @Test
    void survivesClientThatConnectsAndLeaves(ServerUnderTest server) throws Exception {
        server.connect().close();
        assertEquals(200, exchange(server, GET).status(), "Server must keep serving after an empty connection");
    }

    @Test
    void survivesClientThatHangsUpMidRequest(ServerUnderTest server) throws Exception {
        try (var conn = server.connect()) {
            conn.send("GET / HTTP/1.1\\r\\nHo");
        }
        assertEquals(200, exchange(server, GET).status(), "Server must keep serving after a truncated request");
    }

    @Test
    void survivesClientThatHalfClosesMidRequest(ServerUnderTest server) throws Exception {
        try (var conn = server.connect()) {
            conn.send("GET / HTTP/1.1\\r\\nHo").finishSending();
            byte[] raw = conn.readUntilClose(RESPONSE_TIMEOUT); // either nothing or a 400, but it must close
            if (raw.length > 0) assertEquals(400, HttpResponse.parse(raw).status());
        }
        assertEquals(200, exchange(server, GET).status());
    }

    @Test
    void survivesBadRequestThenServesGoodOne(ServerUnderTest server) throws Exception {
        assertEquals(400, exchange(server, "requests/phase1/bad/garbage.req").status());
        assertEquals(200, exchange(server, GET).status());
    }

    @Test
    void servesManyConnectionsInARow(ServerUnderTest server) throws Exception {
        for (int i = 0; i < 50; i++) {
            assertEquals(200, exchange(server, GET).status(), "Failed on connection #" + i);
        }
    }
}
