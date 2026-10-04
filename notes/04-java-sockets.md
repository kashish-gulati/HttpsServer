# 04 — What Java's socket API promises

Prep for Phase 1. Answer from the Javadoc (JDK 25) and small experiments, not from memory.
These are about the *API's contract*; how you use it in the server is yours to design in Phase 1.

## Listening and accepting

1. What's the difference between `new ServerSocket(port)` and `new ServerSocket(port, backlog)`? What
   happens to a client that connects while the backlog is full?

   **A:**

2. Your server crashes and you restart it immediately: "Address already in use". Why (see 02 Q10), and which
   socket option exists for this? Does Java set it by default on Windows vs Linux?

   **A:**

3. Does `accept()` block? What do you get back, and what does it tell you about the client?

   **A:**

4. `localhost`, `127.0.0.1`, `0.0.0.0`: which address should a dev server bind to, and what's the risk of
   the wrong choice?

   **A:**

## Reading

5. ★ `InputStream.read(byte[] buf)` returns an int. List every possible meaning of the return value.
   Is it guaranteed to fill `buf`?

   **A:**

6. ★ How do you know the client has closed its side? How is that different from "no data right now"?

   **A:**

7. What does `socket.setSoTimeout(ms)` do, and what exception do you get? Is the socket still usable after it?

   **A:**

8. Why is `BufferedReader.readLine()` banned in this project? Give at least two concrete ways it would hide or
   mis-handle bytes the RFC cares about.

   **A:**

9. Java's `byte` is signed. What is `(byte) 0xC8` as an int? How do you get 200 back? Where will this matter
   (two places later in the roadmap)?

   **A:**

## Writing and closing

10. Does `OutputStream.write(bytes)` mean the bytes reached the client? What does `flush()` add on a socket
    stream?

    **A:**

11. What's the difference between `shutdownOutput()` and `close()`? When would a server want the first?

    **A:**

12. What happens if you write to a socket the client already closed? (Exception name, and when it shows up —
    first write or a later one?)

    **A:**

## Text vs bytes

13. Which charset should you use to turn header bytes into a `String`, and why not UTF-8?

    **A:**

14. `"\r\n".getBytes()` without a charset argument — what's wrong with relying on the default?

    **A:**

## Threads (preview of Phase 3)

15. In a single-threaded server, what happens to client B while you're reading from client A, who is typing
    slowly in `nc`? Predict it, then verify it once Phase 1 works.

    **A:**
