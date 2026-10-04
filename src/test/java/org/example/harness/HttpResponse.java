package org.example.harness;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A deliberately strict, test-side view of one response. It rejects bare LF line endings so that
 * sloppy server output fails loudly. Only Content-Length bodies are supported; responses without it
 * are read until close via {@link #parse(byte[])}.
 */
public record HttpResponse(String version, int status, String reason, List<Header> headers, byte[] body, byte[] raw) {

    public record Header(String name, String value) {}

    public Optional<String> header(String name) {
        return headers.stream().filter(h -> h.name().equalsIgnoreCase(name)).map(Header::value).findFirst();
    }

    public String bodyText() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /** Parses a complete response captured with {@link RawConnection#readUntilClose()}. */
    public static HttpResponse parse(byte[] raw) {
        int headEnd = indexOf(raw, new byte[] {'\r', '\n', '\r', '\n'});
        if (headEnd < 0) {
            throw new AssertionError("No CRLFCRLF ending the response head. Raw response:\n" + RawBytes.show(raw));
        }
        Head head = parseHead(raw, headEnd);
        byte[] rest = java.util.Arrays.copyOfRange(raw, headEnd + 4, raw.length);
        Optional<Integer> length = contentLength(head.headers, raw);
        if (length.isPresent() && length.get() != rest.length) {
            throw new AssertionError("Content-Length is " + length.get() + " but " + rest.length
                    + " body bytes followed. Raw response:\n" + RawBytes.show(raw));
        }
        return new HttpResponse(head.version, head.status, head.reason, head.headers, rest, raw);
    }

    /**
     * Parses a response to HEAD: headers describe the body GET would return (Content-Length included),
     * but no body bytes may follow.
     */
    public static HttpResponse parseHeadResponse(byte[] raw) {
        int headEnd = indexOf(raw, new byte[] {'\r', '\n', '\r', '\n'});
        if (headEnd < 0) {
            throw new AssertionError("No CRLFCRLF ending the response head. Raw response:\n" + RawBytes.show(raw));
        }
        if (headEnd + 4 != raw.length) {
            throw new AssertionError("A response to HEAD must not carry a body, but " + (raw.length - headEnd - 4)
                    + " bytes followed the head:\n" + RawBytes.show(raw));
        }
        Head head = parseHead(raw, headEnd);
        return new HttpResponse(head.version, head.status, head.reason, head.headers, new byte[0], raw);
    }

    static HttpResponse readFrom(InputStream in) throws IOException {
        ByteArrayOutputStream headBuf = new ByteArrayOutputStream();
        int matched = 0;
        while (matched < 4) {
            int b = in.read();
            if (b == -1) {
                throw new EOFException("Connection closed before end of response head. Got:\n"
                        + RawBytes.show(headBuf.toByteArray()));
            }
            headBuf.write(b);
            matched = (b == (matched % 2 == 0 ? '\r' : '\n')) ? matched + 1 : (b == '\r' ? 1 : 0);
        }
        byte[] headBytes = headBuf.toByteArray();
        Head head = parseHead(headBytes, headBytes.length - 4);
        int length = contentLength(head.headers, headBytes).orElseThrow(() -> new AssertionError(
                "readResponse() needs Content-Length to know where the body ends. Head:\n" + RawBytes.show(headBytes)));
        byte[] body = in.readNBytes(length);
        if (body.length != length) {
            throw new EOFException("Expected " + length + " body bytes, got " + body.length);
        }
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        raw.writeBytes(headBytes);
        raw.writeBytes(body);
        return new HttpResponse(head.version, head.status, head.reason, head.headers, body, raw.toByteArray());
    }

    private record Head(String version, int status, String reason, List<Header> headers) {}

    private static Head parseHead(byte[] raw, int headEnd) {
        String head = new String(raw, 0, headEnd, StandardCharsets.ISO_8859_1);
        if (head.replace("\r\n", "").indexOf('\n') >= 0) {
            throw new AssertionError("Response head contains a bare LF (lines must end in CRLF):\n" + RawBytes.show(raw));
        }
        String[] lines = head.split("\r\n", -1);
        String[] statusLine = lines[0].split(" ", 3);
        if (statusLine.length < 2 || !statusLine[0].startsWith("HTTP/") || !statusLine[1].matches("\\d{3}")) {
            throw new AssertionError("Malformed status line: \"" + lines[0] + "\"");
        }
        List<Header> headers = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon <= 0) throw new AssertionError("Malformed header line: \"" + lines[i] + "\"");
            headers.add(new Header(lines[i].substring(0, colon), lines[i].substring(colon + 1).strip()));
        }
        return new Head(statusLine[0], Integer.parseInt(statusLine[1]),
                statusLine.length > 2 ? statusLine[2] : "", List.copyOf(headers));
    }

    private static Optional<Integer> contentLength(List<Header> headers, byte[] raw) {
        List<String> values = headers.stream().filter(h -> h.name().equalsIgnoreCase("Content-Length"))
                .map(Header::value).toList();
        if (values.isEmpty()) return Optional.empty();
        if (values.stream().distinct().count() > 1) {
            throw new AssertionError("Conflicting Content-Length headers: " + values + "\n" + RawBytes.show(raw));
        }
        try {
            return Optional.of(Integer.parseInt(values.get(0)));
        } catch (NumberFormatException e) {
            throw new AssertionError("Content-Length is not a number: \"" + values.get(0) + "\"");
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
