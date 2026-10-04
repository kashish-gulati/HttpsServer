package org.example.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public abstract class MessageHead {
    private String httpVersion;
    private Map<String,String> headers;
    public MessageHead(String httpVersion, Map<String,String> headers){
        this.httpVersion=httpVersion;
        this.headers=headers;
    }
}
