package org.example.harness;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Gives each test class a server to talk to. Two modes:
 * <ul>
 *   <li><b>Launch</b> (default): starts {@code server.main} (default {@code org.example.Main}) in a separate JVM
 *       as {@code java org.example.Main <port>} and waits until the port accepts connections.
 *       The server contract is just that: read the port from {@code args[0]} and listen on it.</li>
 *   <li><b>Attach</b>: with {@code -Dserver.port=8080}, connects to a server you already started yourself,
 *       e.g. from IntelliJ under the debugger.</li>
 * </ul>
 * Inject {@link ServerUnderTest} into a test method to get the host and port.
 */
public final class ServerExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(5);
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(ServerExtension.class);

    /** {@code process} and {@code log} are null in attach mode. */
    public record ServerUnderTest(String host, int port, Process process, Path log) {
        public RawConnection connect() throws IOException {
            try {
                return new RawConnection(host, port);
            } catch (ConnectException e) {
                if (process != null && waitForExit(process)) {
                    // Note: the startup readiness probe is itself an empty connection (connect, then close).
                    throw new AssertionError("Connection refused because the server process has exited (code "
                            + process.exitValue() + "). It was running when the tests started, so an earlier"
                            + " connection killed it, possibly the startup probe, which connects and closes"
                            + " without sending anything.\n--- server output ---\n" + Files.readString(log), e);
                }
                throw e;
            }
        }

        /** The process may still be shutting down when the connect is refused; give it a moment. */
        private static boolean waitForExit(Process process) {
            try {
                return process.waitFor(1, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return !process.isAlive();
            }
        }
    }

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        String attachPort = System.getProperty("server.port");
        if (attachPort != null) {
            ServerUnderTest server = new ServerUnderTest("localhost", Integer.parseInt(attachPort), null, null);
            if (!accepts(server.port())) {
                throw new AssertionError("Attach mode: nothing is listening on localhost:" + attachPort
                        + ". Start your server first, or drop -Dserver.port to let the tests launch it.");
            }
            ctx.getStore(NS).put("server", server);
            return;
        }

        String mainClass = System.getProperty("server.main", "org.example.Main");
        int port = freePort();
        Path log = Files.createTempFile("server-under-test-", ".log");
        Process process = new ProcessBuilder(
                ProcessHandle.current().info().command().orElse("java"),
                "-cp", System.getProperty("java.class.path"),
                mainClass, String.valueOf(port))
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        ctx.getStore(NS).put("process", process);
        ctx.getStore(NS).put("log", log);

        long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
        while (!accepts(port)) {
            if (!process.isAlive()) {
                throw new AssertionError("Server process exited with code " + process.exitValue()
                        + " before listening on port " + port + ".\nIt was started as: java " + mainClass + " " + port
                        + "\n--- server output ---\n" + Files.readString(log));
            }
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Server (" + mainClass + ") did not accept connections on port " + port
                        + " within " + STARTUP_TIMEOUT + ". Does it listen on args[0]?"
                        + "\n--- server output ---\n" + Files.readString(log));
            }
            Thread.sleep(50);
        }
        ctx.getStore(NS).put("server", new ServerUnderTest("localhost", port, process, log));
    }

    @Override
    public void afterAll(ExtensionContext ctx) throws Exception {
        Process process = ctx.getStore(NS).get("process", Process.class);
        if (process == null) return;
        process.destroy();
        if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
        Path log = ctx.getStore(NS).get("log", Path.class);
        String output = Files.readString(log);
        if (!output.isBlank()) System.out.println("--- server output (" + ctx.getDisplayName() + ") ---\n" + output);
        Files.deleteIfExists(log);
    }

    @Override
    public boolean supportsParameter(ParameterContext p, ExtensionContext ctx) {
        return p.getParameter().getType() == ServerUnderTest.class;
    }

    @Override
    public Object resolveParameter(ParameterContext p, ExtensionContext ctx) {
        return ctx.getStore(NS).get("server", ServerUnderTest.class);
    }

    private static boolean accepts(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", port), 200);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
