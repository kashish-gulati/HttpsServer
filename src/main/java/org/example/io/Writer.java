package org.example.io;

import org.example.model.request.Request;
import org.example.model.request.RequestHead;
import org.example.model.request.RequestMethod;
import org.example.model.response.Response;
import org.example.model.response.ResponseHead;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Writer {
    private InputStream in;
    private OutputStream out;

    public Writer(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public void send(Response response) throws IOException {
        if(response==null) return;
        sendResponseHead(response.getHead());
        //todo:send body
    }

    private void sendResponseHead(ResponseHead responseHead) throws IOException {
        // HTTP-version SP status-code SP [ reason-phrase ]
        String statusLine = responseHead.getHttpVersion() + " " + responseHead.getStatus()+" ";//todo: maybe add reason phrase in future
        out.write(statusLine.getBytes(StandardCharsets.US_ASCII));
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        sendHeaders(responseHead.getHeaders());
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
    }

    private void sendHeaders(Map<String,String> headers) throws IOException {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            out.write(entry.getKey().getBytes(StandardCharsets.US_ASCII));
            out.write(": ".getBytes(StandardCharsets.US_ASCII));
            out.write(entry.getValue().getBytes(StandardCharsets.US_ASCII));
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }
    }
}
