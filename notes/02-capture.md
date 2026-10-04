# 02 — One request on the wire

Setup (WSL, two terminals):

```bash
# Terminal 1
sudo tcpdump -i eth0 -w ~/http.pcap 'host example.com and tcp port 80'
# Terminal 2
curl -v http://example.com/ -o /dev/null
# Terminal 1: Ctrl+C, then
tshark -r ~/http.pcap
tshark -r ~/http.pcap -q -z follow,tcp,ascii,0
```

Paste the `tshark -r ~/http.pcap` summary here so the answers below can point at packet numbers:

```
(paste here)
```

## TCP connection

1. Which packets are the three-way handshake? What are the client and server ports, and why is the client's
   port a large random number?

   **A:**

2. What MSS did each side advertise in its SYN? What does MSS limit?

   **A:**

3. In your server, which Java call returns once this handshake is complete — and does your code take part in
   the handshake at all?

   **A:**

## The request

4. Which packet carries the GET? How many bytes is its TCP payload? Account for every byte (request line +
   each header + CRLFs).

   **A:**

5. Compare with `curl -v`'s `>` lines. Which headers did curl add that you didn't ask for?

   **A:**

## The response

6. How many TCP segments did the response take? Why isn't it one packet?

   **A:**

7. ★ If your server called `read()` once on a connection like this, what are all the things it could get back?
   (Think: less than one request, exactly one, more than one.)

   **A:**

8. Pick one ACK. Show how its ack number = the other side's seq + bytes received.

   **A:**

## Closing

9. Who sent the first FIN, and why that side? What would change with `Connection: close`?

   **A:**

10. What is TIME_WAIT, which side ends up in it, and why might it bite you when you restart your server quickly?

    **A:**

## Same thing over HTTPS

11. Capture `curl https://example.com/` on port 443. What's the first packet after the TCP handshake called?
    Can you find the hostname in it in plain text? Why is that a privacy issue?

    **A:**

12. How many round trips happen before the first byte of HTTP is sent, for http vs https (TLS 1.3)?

    **A:**
