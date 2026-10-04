package org.example.harness;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Checks the harness itself, so a red test always means the server, not the tooling. */
class HarnessSelfTest {

    @Test
    void escapesDecodeToExactBytes() {
        byte[] bytes = RawBytes.fromEscaped("GET / HTTP/1.1\\r\\n\nHost: x\\r\\n\n\\r\\n\n\\x00\\xFF\\\\");
        byte[] expected = "GET / HTTP/1.1\r\nHost: x\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
        byte[] tail = {0, (byte) 0xFF, '\\'};
        byte[] all = new byte[expected.length + tail.length];
        System.arraycopy(expected, 0, all, 0, expected.length);
        System.arraycopy(tail, 0, all, expected.length, tail.length);
        assertArrayEquals(all, bytes);
    }

    @Test
    void requestFileLoads() {
        assertEquals("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n",
                new String(RawBytes.resource("requests/simple-get.req"), StandardCharsets.ISO_8859_1));
    }

    @Test
    void parsesWellFormedResponse() {
        HttpResponse r = HttpResponse.parse(bytes("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nX-A:  b \r\n\r\nhi"));
        assertEquals(200, r.status());
        assertEquals("OK", r.reason());
        assertEquals("b", r.header("x-a").orElseThrow());
        assertEquals("hi", r.bodyText());
    }

    @Test
    void readsOneResponseOffAStreamAndLeavesTheRest() throws Exception {
        var in = new ByteArrayInputStream(bytes(
                "HTTP/1.1 200 OK\r\nContent-Length: 1\r\n\r\naHTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"));
        assertEquals("a", HttpResponse.readFrom(in).bodyText());
        assertEquals(404, HttpResponse.readFrom(in).status());
    }

    @Test
    void rejectsBareLf() {
        assertThrows(AssertionError.class, () -> HttpResponse.parse(bytes("HTTP/1.1 200 OK\nContent-Length: 0\r\n\r\n")));
    }

    @Test
    void rejectsWrongContentLength() {
        assertThrows(AssertionError.class, () -> HttpResponse.parse(bytes("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhi")));
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }
}
