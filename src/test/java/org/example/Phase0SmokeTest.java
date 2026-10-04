package org.example;

import org.example.harness.HttpResponse;
import org.example.harness.RawBytes;
import org.example.harness.ServerExtension;
import org.example.harness.ServerExtension.ServerUnderTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 0 exit check: this is expected to FAIL until Phase 1 exists, with a clear message saying the
 * server never started listening. Once it passes, you've got a server that answers a simple GET.
 */
@ExtendWith(ServerExtension.class)
class Phase0SmokeTest {

    @Test
    void answersSimpleGet(ServerUnderTest server) throws Exception {
        try (var conn = server.connect()) {
            conn.send(RawBytes.resource("requests/simple-get.req"));
            HttpResponse response = conn.readResponse();
            assertEquals("HTTP/1.1", response.version());
            assertEquals(200, response.status(), () -> RawBytes.show(response.raw()));
        }
    }
}
