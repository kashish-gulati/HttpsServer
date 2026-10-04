package org.example.model.request;

import lombok.*;
import org.example.model.MessageHead;

import java.util.Map;

@Getter
@Setter
@ToString
public class RequestHead extends MessageHead {
    private RequestMethod method;
    private String requestTarget;
    private Map<String,String> headers;
    public RequestHead(String httpVersion, Map<String,String> headers, RequestMethod method, String requestTarget) {
        super(httpVersion,headers);
        this.method=method;
        this.requestTarget=requestTarget;
    }
}
