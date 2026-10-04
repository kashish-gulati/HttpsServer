# 01 — Talking HTTP by hand

Setup (WSL terminal). Paste requests rather than type them: example.com drops connections that
haven't finished the request head within ~15 s.

```bash
nc -C -v example.com 80
openssl s_client -connect example.com:443 -servername example.com -crlf
python3 -m http.server 8000      # a patient local server, for slow experiments
```

> **Review 1 (2026-10-03):** 9 correct · 5 partial · 4 to redo (Q10, Q12, Q14, Q15) · Q17 open.
> Observations were all accurate; the gaps are in the *why*. Reviews are in the quote blocks under each
> answer. Your answers are left as you wrote them. When you revise, write under the review, don't delete it.

## Plain HTTP with nc

1. What does `-C` change about the bytes `nc` sends? Why does HTTP care?

   **A:** I think it changes LF to CRLF

   > 🟡 **Partial.** Right: Enter sends `\r\n` instead of `\n`. Missing the *why*: RFC 9112 defines CRLF as
   > the line terminator; accepting bare LF is only a MAY. A strict server that splits on `\r\n` would never
   > see your request end.

2. Why does the server not answer until you send the empty line? What does the empty line mean?

   **A:** Because the empty line is a separator to let the server know that all headers have been sent.

   > ✅ **Correct.** Refinement: it ends the *head*, not the *message*. A body may follow. Your readerWriter needs
   > to keep "head is complete" and "message is complete" as separate states.

3. Paste the status line and header names you got back (values optional). Which header tells you how the
   body's end is marked?

   **A:**
   HTTP/1.1 200 OK
   Date: Sat, 03 Oct 2026 16:04:10 GMT
   Content-Type: text/html; charset=utf-8
   Transfer-Encoding: chunked
   Connection: keep-alive
   Server: cloudflare
   Last-Modified: Fri, 02 Oct 2026 16:11:02 GMT
   Allow: GET, HEAD
   Accept-Ranges: bytes
   Age: 5938
   cf-cache-status: HIT
   CF-RAY: a44d2e51eb693c12-BLR
   alt-svc: h3=":443"; ma=86400
    is it Transfer Encoding

   > ✅ **Correct.** `Transfer-Encoding: chunked`. The three ways a body's end can be marked are
   > `Content-Length`, chunked, or (responses only) the server closing the connection. See Q10.

4. ★ The response used `Transfer-Encoding: chunked`. Look at the raw body: what do the hex lines mean,
   and how does the client know the body is finished?

   **A:** Is it from the 0 at the end of the request

   > 🟡 **Partial.** The `0` is at the end of the *response* body, not the request. Full rule: each chunk is
   > `<size in hex>\r\n<exactly that many bytes>\r\n`. A chunk of size `0` means "no more chunks", followed
   > by optional trailer fields and a final `\r\n`. Example: `1f4` = 500 bytes follow. (Phase 2 material.)

5. After the response, is the connection still open? Send a second request on it. What did you see?

   **A:** Yes, it is open. when I send a second request, I get a response again

   > ✅ **Correct.** Note *why* this works: the client can find the end of response 1 (via chunked framing)
   > without the connection closing. Keep-alive is only possible because the body framing is explicit.

6. ★ Send the request without a `Host:` line. Status code and reason?

   **A:** Its 400 Bad Request

   > ✅ **Correct.** And the RFC requires it: an HTTP/1.1 request without Host MUST get 400 (RFC 9112 §3.2).

7. ★ Send `get / HTTP/1.1` (lowercase method). What happened, and what do you think *should* happen?

   **A:** Bad reqeust again which shoudl happen as http methods are case sensitive

   > 🟡 **Partial.** The observation is right. On the reasoning: because methods are case-sensitive,
   > `get` isn't *malformed*. It's a *different, unknown* method. RFC 9110 §9.1: a server receiving an
   > unrecognized method SHOULD respond **501 Not Implemented**. Cloudflare chose 400. Your server: 501.

8. Add `Connection: close`. What changes in the response headers, and what does `nc` do afterwards?

   **A:** response header also sends Connection: close, and nc closes the connection after receiving the response

   > ✅ **Correct.** Precisely: the *server* closes after responding, `nc` reads EOF and exits.

9. ★ Run `nc` without `-C` so lines end in bare `\n`. Did example.com still accept it? Did
   `python3 -m http.server`?

   **A:** example.com did. python serrver as well

   > ✅ **Correct.** Reproduced: example.com returns 200 for bare-LF requests. 03 Q5 has the RFC rule that
   > permits this leniency. Your server's policy: accept bare LF as a line end; reject a bare CR (03 Q6).

10. ★ Send `GET / HTTP/1.0` with no Host header. What happened? Why might 1.0 be treated differently?

    **A:** Getting forbidden instead of bad request, maybe since example,.com doesnt support HTTP/1.0

    > ❌ **Redo.** The observation is right, but the explanation isn't. Reproduced:
    >
    > | Request | Result |
    > |---|---|
    > | `GET / HTTP/1.0`, no Host | `403 Forbidden` |
    > | `GET / HTTP/1.0`, `Host: example.com` | **`200 OK`** |
    >
    > So 1.0 is supported. The 403 is about the missing Host: Cloudflare serves millions of sites from shared
    > IPs and can't tell which one you want. It's not 400 because Host is only *required* from 1.1.
    > Re-run with Host and explain two things: (a) the status line says `HTTP/1.1` though you sent 1.0, so
    > why is that allowed (03 Q11)? (b) the response has `Connection: close` and **neither** Content-Length
    > nor chunked, so how does a 1.0 client find the end of the body?

11. Against `python3 -m http.server`, type the request slowly. What version does it answer with, and does it
    keep the connection open? Why might that be?

    **A:** It responds with HTTP/1.0 and it does not keep the connection open. Is it because 1.0 does not support keeping connection open?

    > 🟡 **Partial.** The version is right. 1.0 *can* keep connections open, but only if the client opts in
    > with `Connection: keep-alive`. The defaults are flipped: **1.0 defaults to close, 1.1 defaults to
    > persistent**. Python's `BaseHTTPRequestHandler` speaks 1.0 unless configured otherwise.

12. Why did `nc` seem to "exit on Enter" earlier? Explain it in terms of who closed the connection and when.

    **A:** Because the server closed the connection when it sent the response

    > ❌ **Redo.** No response was ever sent. Sequence: you took more than ~15 s to type the head →
    > Cloudflare closed the connection *while you were still typing* (a header-read timeout, an anti-Slowloris
    > defence) → your Enter wrote to a dead connection → `nc` exited silently. Measured: 12 s pause → 200,
    > 16 s pause → closed, no reply. Rewrite contrasting this with Q8's normal close-after-response.

## HTTPS with openssl s_client

13. Which TLS version and cipher suite were negotiated? (Copy the lines.)

    **A:** TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384

    > ✅ **Correct.** Bonus to look up: what do the three parts AES_256 / GCM / SHA384 each do?

14. List the certificate chain you received (depth 0 … n). Which one is the server's own certificate, and which
    one did your machine already trust?

    **A:**
    Certificate chain
    0 s:CN = example.com
    i:C = US, O = SSL Corporation, CN = Cloudflare TLS Issuing ECC CA 3
    a:PKEY: id-ecPublicKey, 256 (bit); sigalg: ecdsa-with-SHA256
    v:NotBefore: Sep 26 22:49:11 2026 GMT; NotAfter: Dec 25 22:56:35 2026 GMT
    1 s:C = US, O = SSL Corporation, CN = Cloudflare TLS Issuing ECC CA 3
    i:C = US, O = SSL Corporation, CN = SSL.com TLS Transit ECC CA R2
    a:PKEY: id-ecPublicKey, 256 (bit); sigalg: ecdsa-with-SHA384
    v:NotBefore: May 29 19:49:45 2025 GMT; NotAfter: May 27 19:49:44 2035 GMT
    2 s:C = US, O = SSL Corporation, CN = SSL.com TLS Transit ECC CA R2
    i:C = US, O = SSL Corporation, CN = SSL.com TLS ECC Root CA 2022
    a:PKEY: id-ecPublicKey, 384 (bit); sigalg: ecdsa-with-SHA384
    v:NotBefore: Oct 21 17:02:23 2022 GMT; NotAfter: Oct 17 17:02:22 2037 GMT
    3 s:C = US, O = SSL Corporation, CN = SSL.com TLS ECC Root CA 2022
    i:C = GB, ST = Greater Manchester, L = Salford, O = Comodo CA Limited, CN = AAA Certificate Services
    a:PKEY: id-ecPublicKey, 384 (bit); sigalg: RSA-SHA256
    v:NotBefore: Aug  1 00:00:00 2025 GMT; NotAfter: Dec 31 23:59:59 2028 GMT
    
    The 0th one, my machine trusted all four

    > ❌ **Redo the second half.** Depth 0 is correct. But your machine trusts **only a root in its trust
    > store**; every other cert is trusted because the one above it *signed* it. Checked: WSL's
    > `/etc/ssl/certs` contains `SSL.com_TLS_ECC_Root_CA_2022.pem`, the trust anchor.
    > Twist: the depth-3 cert the server *sent* is issued by Comodo's "AAA Certificate Services". It's a
    > **cross-signed** copy so old clients that only trust the Comodo root still work. OpenSSL used its own
    > self-signed copy from the store instead. Look up cross-signing and rewrite.

15. What does `-servername` do? Run it once without it. Did anything change?

    **A:** I'm gussing servername specifies the exact server at a host. Running without didn't change anything as I'm guessing exampl.com is the default server for the example.com host.

    > ❌ **Redo with a new experiment.** `-servername` sets **SNI** (Server Name Indication): the hostname,
    > sent **in plaintext inside the Client Hello**, so a server hosting many sites on one IP can pick the
    > right certificate *before* encryption starts. It's TLS's equivalent of the Host header (compare Q6/Q10).
    > Nothing changed because **OpenSSL 3 sends SNI automatically** from the `-connect` host. To really turn
    > it off: `openssl s_client -connect example.com:443 -noservername`. Reproduced: Cloudflare aborts with
    > `sslv3 alert handshake failure` (alert 40). Run it and explain why.

16. Once the handshake was done, how did the HTTP you typed differ from the plain-text version?

    **A:** No diff

    > ✅ **Correct.** This is the key insight for Phase 4's design (Q18).

17. In one sentence: what does TLS give you that plain TCP doesn't? Name all three properties.

    **A:** Not sure

    > ⬜ **Open.** Hint: **confidentiality**, **integrity**, **authentication**. In your own words, say what
    > attack each one prevents, and which part of the `s_client` output is evidence for each (the certificate
    > chain is one of them).

18. Design thought: given 16, how should your server's code be split so that adding TLS in Phase 4 doesn't touch
    your HTTP readerWriter?

    **A:** We can have the decision making logic separate from the parsing logic for http.

    > 🟡 **Partial, too vague.** Concretely: the HTTP layer should consume an `InputStream` and write an
    > `OutputStream` and never know whether those come from a raw `Socket` or a TLS layer. In Phase 4, TLS
    > becomes a wrapper that takes the socket's streams and hands back decrypted ones. Rewrite with that
    > layering, and keep it in mind while writing Phase 1.
