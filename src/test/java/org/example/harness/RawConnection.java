package org.example.harness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;

/**
 * A plain TCP client that sends exact bytes and returns exact bytes. It knows nothing about HTTP,
 * so it can send anything, including malformed or fragmented requests.
 */
public final class RawConnection implements AutoCloseable {
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(3);

    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;

    public RawConnection(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 2000);
        socket.setTcpNoDelay(true); // so sendInChunks really produces separate segments
        socket.setSoTimeout((int) DEFAULT_READ_TIMEOUT.toMillis());
        out = socket.getOutputStream();
        in = socket.getInputStream();
    }

    public RawConnection send(byte[] bytes) throws IOException {
        out.write(bytes);
        out.flush();
        return this;
    }

    public RawConnection send(String escaped) throws IOException {
        return send(RawBytes.fromEscaped(escaped));
    }

    /** Sends the bytes in pieces of {@code chunkSize}, pausing between them, to exercise partial reads. */
    public RawConnection sendInChunks(byte[] bytes, int chunkSize, Duration pause) throws IOException {
        for (int off = 0; off < bytes.length; off += chunkSize) {
            out.write(bytes, off, Math.min(chunkSize, bytes.length - off));
            out.flush();
            sleep(pause);
        }
        return this;
    }

    /** Half-closes: the server sees EOF on read, but we can still read its response. */
    public RawConnection finishSending() throws IOException {
        socket.shutdownOutput();
        return this;
    }

    /** Reads until the server closes the connection, failing if it takes longer than {@code timeout}. */
    public byte[] readUntilClose(Duration timeout) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        long deadline = System.nanoTime() + timeout.toNanos();
        byte[] chunk = new byte[8192];
        while (true) {
            try {
                int n = in.read(chunk);
                if (n == -1) return buf.toByteArray();
                buf.write(chunk, 0, n);
            } catch (SocketTimeoutException e) {
                // fall through to the deadline check
            }
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Server did not close the connection within " + timeout
                        + ". Received so far:\n" + RawBytes.show(buf.toByteArray()));
            }
        }
    }

    public byte[] readUntilClose() throws IOException {
        return readUntilClose(DEFAULT_READ_TIMEOUT);
    }

    /**
     * Reads exactly one HTTP response (head + Content-Length body) and leaves the connection open,
     * so keep-alive tests can send the next request on the same socket.
     */
    public HttpResponse readResponse() throws IOException {
        return HttpResponse.readFrom(in);
    }

    /** True if the server has closed its side (read returns EOF within {@code wait}). */
    public boolean serverClosed(Duration wait) throws IOException {
        socket.setSoTimeout((int) wait.toMillis());
        try {
            return in.read() == -1;
        } catch (SocketTimeoutException e) {
            return false;
        } finally {
            socket.setSoTimeout((int) DEFAULT_READ_TIMEOUT.toMillis());
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
