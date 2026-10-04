package org.example;

import org.example.model.request.Request;
import org.example.model.response.Response;
import org.example.model.response.ResponseHead;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.example.DateTimeUtil.RFC_1123_DATE_TIME_FORMATTER;

public class Server {
//    private ReaderWriter readerWriter;
//    public Server(ReaderWriter readerWriter) {
//        this.readerWriter = readerWriter;
//    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("");

    public Response respond(Request request) {
        //todo: improve logic
        if(request.getHead()==null) return null;
        Map<String,String> headers = Map.of("Content-Length","0",
                "Connection","close",
                "Date", RFC_1123_DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
        ResponseHead responseHead = new ResponseHead("HTTP/1.1",headers,200);
        return new Response(responseHead);
    }
}
