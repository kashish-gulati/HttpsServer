package org.example.phase1;

import org.example.harness.HttpResponse;
import org.example.harness.RawBytes;
import org.example.harness.ServerExtension;
import org.example.harness.ServerExtension.ServerUnderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One raw request file in, one status code out. The "why" column is the rule behind the expected status;
 * policy choices (where the RFC allows more than one reaction) are marked "policy".
 * Files live in src/test/resources/requests/phase1/{good,bad}/.
 */
@ExtendWith(ServerExtension.class)
class Phase1RequestFilesTest {

    @ParameterizedTest(name = "{0} -> {1}  ({2})")
    @CsvSource(delimiter = '|', textBlock = """
            good/many-headers.req            | 200 | unknown headers are ignored; value may be empty or contain ':'
            good/header-name-case.req        | 200 | field names are case-insensitive (RFC 9110 §5.1)
            good/header-no-space.req         | 200 | OWS after the colon is optional (RFC 9112 §5)
            good/header-extra-whitespace.req | 200 | leading/trailing OWS is not part of the value (RFC 9112 §5)
            good/bare-lf.req                 | 200 | policy: accept bare LF as a line end (RFC 9112 §2.2 MAY)
            good/leading-crlf.req            | 200 | ignore empty lines before the request line (RFC 9112 §2.2 SHOULD)
            good/absolute-form.req           | 200 | origin servers MUST accept absolute-form (RFC 9112 §3.2.2)
            good/http10-no-host.req          | 200 | Host is only required from HTTP/1.1; answer with HTTP/1.1 (RFC 9110 §2.5)
            good/query-string.req            | 200 | the query is not part of the path
            good/not-found.req               | 404 | nothing is served at this path
            """)
    void goodRequests(String file, int status, String why, ServerUnderTest server) throws Exception {
        expect(server, file, status);
    }

    @ParameterizedTest(name = "{0} -> {1}  ({2})")
    @CsvSource(delimiter = '|', textBlock = """
            bad/missing-host.req                   | 400 | HTTP/1.1 without Host MUST get 400 (RFC 9112 §3.2)
            bad/duplicate-host.req                 | 400 | more than one Host MUST get 400 (RFC 9112 §3.2)
            bad/lowercase-method.req               | 501 | methods are case-sensitive, so 'get' is unknown (RFC 9110 §9.1)
            bad/unknown-method.req                 | 501 | unrecognized method SHOULD get 501 (RFC 9110 §9.1)
            bad/version-2.0.req                    | 505 | major version not supported (RFC 9110 §15.6.6)
            bad/version-3.1.req                    | 505 | major version not supported (RFC 9110 §15.6.6)
            bad/version-lowercase.req              | 400 | HTTP-name is case-sensitive (RFC 9112 §2.3)
            bad/version-two-digit-minor.req        | 400 | version is exactly DIGIT '.' DIGIT (RFC 9112 §2.3)
            bad/version-no-minor.req               | 400 | version is exactly DIGIT '.' DIGIT (RFC 9112 §2.3)
            bad/version-missing.req                | 400 | HTTP/0.9-style request line is not valid HTTP/1.1
            bad/double-space.req                   | 400 | policy: exactly one SP between parts (RFC 9112 §3)
            bad/tab-separator.req                  | 400 | policy: exactly one SP between parts (RFC 9112 §3)
            bad/trailing-space-request-line.req    | 400 | policy: nothing after the version (RFC 9112 §3)
            bad/space-in-target.req                | 400 | whitespace in request-target is invalid (RFC 9112 §3)
            bad/target-no-slash.req                | 400 | origin-form must start with '/' (RFC 9112 §3.2.1)
            bad/bare-cr.req                        | 400 | policy: bare CR is invalid (RFC 9112 §2.2 MUST reject or replace)
            bad/whitespace-before-first-header.req | 400 | policy: reject (RFC 9112 §2.2 MUST reject or ignore line)
            bad/header-no-colon.req                | 400 | field-line needs ':' (RFC 9112 §5)
            bad/space-before-colon.req             | 400 | whitespace before ':' MUST get 400 (RFC 9112 §5.1)
            bad/empty-header-name.req              | 400 | field-name is a non-empty token (RFC 9110 §5.1)
            bad/obs-fold.req                       | 400 | policy: reject line folding (RFC 9112 §5.2 MAY)
            bad/control-char-in-value.req          | 400 | NUL is not allowed in a field value (RFC 9110 §5.5)
            bad/garbage.req                        | 400 | e.g. a TLS ClientHello sent to the plain-HTTP port
            """)
    void badRequests(String file, int status, String why, ServerUnderTest server) throws Exception {
        expect(server, file, status);
    }

    private static void expect(ServerUnderTest server, String file, int status) throws Exception {
        HttpResponse r = Phase1Support.exchange(server, "requests/phase1/" + file);
        assertEquals(status, r.status(), () -> "Wrong status for " + file + "\n--- response ---\n" + RawBytes.show(r.raw()));
    }
}
