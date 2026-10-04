package org.example.harness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Converts between the escaped text used in .req files and the exact bytes sent on the wire.
 *
 * <p>Escaped format: physical line breaks in the file are ignored, so every byte must be spelled out.
 * Supported escapes: {@code \r \n \t \0 \\ \xHH}. Anything else is taken as-is (ISO-8859-1).
 * <pre>
 * GET / HTTP/1.1\r\n
 * Host: localhost\r\n
 * \r\n
 * </pre>
 */
public final class RawBytes {
    private RawBytes() {}

    public static byte[] fromEscaped(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') continue; // physical newlines are formatting only
            if (c != '\\') {
                if (c > 0xFF) throw new IllegalArgumentException("Non-Latin-1 char at " + i + "; use \\xHH");
                out.write(c);
                continue;
            }
            if (++i >= text.length()) throw new IllegalArgumentException("Dangling backslash at end");
            char e = text.charAt(i);
            switch (e) {
                case 'r' -> out.write('\r');
                case 'n' -> out.write('\n');
                case 't' -> out.write('\t');
                case '0' -> out.write(0);
                case '\\' -> out.write('\\');
                case 'x' -> {
                    if (i + 2 >= text.length())
                        throw new IllegalArgumentException("Truncated \\x escape at " + i);
                    out.write(Integer.parseInt(text.substring(i + 1, i + 3), 16));
                    i += 2;
                }
                default -> throw new IllegalArgumentException("Unknown escape \\" + e + " at " + i);
            }
        }
        return out.toByteArray();
    }

    /** Loads a classpath resource such as {@code requests/simple-get.req} and decodes its escapes. */
    public static byte[] resource(String path) {
        try (InputStream in = RawBytes.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("No test resource: " + path);
            return fromEscaped(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Renders bytes readably for failure messages: CRLF shown as \r\n, other control bytes as \xHH. */
    public static String show(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int v = b & 0xFF;
            switch (v) {
                case '\r' -> sb.append("\\r");
                case '\n' -> sb.append("\\n\n"); // keep a real newline so output stays readable
                case '\t' -> sb.append("\\t");
                case '\\' -> sb.append("\\\\");
                default -> {
                    if (v < 0x20 || v >= 0x7F) sb.append(String.format("\\x%02X", v));
                    else sb.append((char) v);
                }
            }
        }
        return sb.toString();
    }
}
