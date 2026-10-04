package org.example.model.response;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
public class Response {
    private ResponseHead head;
    private String body;

    public Response(ResponseHead responseHead) {
        this.head = responseHead;
    }
}
