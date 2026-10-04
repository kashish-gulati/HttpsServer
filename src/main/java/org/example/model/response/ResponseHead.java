package org.example.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.model.MessageHead;

import java.util.Map;

@Getter
@Setter
@ToString
public class ResponseHead extends MessageHead {
    private int status;

    public ResponseHead(String httpVersion, Map<String,String> headers, int status) {
        super(httpVersion, headers);
        this.status = status;
    }
}
