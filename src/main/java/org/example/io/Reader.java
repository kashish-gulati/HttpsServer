package org.example.io;

import org.example.model.request.Request;
import org.example.model.request.RequestHead;
import org.example.model.request.RequestMethod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Reader {
    private final InputStream in;
    private static final int CHUNK_SIZE = 8;
    private static final int MAX_HEAD_SIZE = 8192;
    private static final byte SP = '\s';
    private static final byte CR = '\r';
    private static final byte LF = '\n';
    public Reader(InputStream in) {
        this.in = in;
    }
    public Request receive() throws IOException {
        Request.RequestBuilder builder = Request.builder();
        builder.head(readRequestHead());
        //todo:body reading to be implemented later
        return builder.build();
    }
    private RequestHead readRequestHead() throws IOException {
        //read request line
        //method SP request-target SP HTTP-version
        byte[] headBytes = new byte[MAX_HEAD_SIZE];
        int rll=0;//reqPointer
        //read method
        //read httpversion
        int readSize;
        while((readSize=in.read(headBytes,rll,CHUNK_SIZE))!=-1
                && (rll+=readSize) < MAX_HEAD_SIZE
                && !checkHeadDelim(headBytes,rll-readSize,rll));//todo: fix reading of request beyond head size
        if(rll<1) return null;
        int readPointer = -1;
        while(readPointer<rll && headBytes[++readPointer]!=SP);
        int elStart=0;
        RequestMethod requestMethod = RequestMethod.valueOf(new String(headBytes,elStart,readPointer-elStart, StandardCharsets.US_ASCII));
        elStart=readPointer+1;
        while(readPointer<rll && headBytes[++readPointer]!=SP);
        String requestTarget = new String(headBytes,elStart,readPointer-elStart, StandardCharsets.US_ASCII);
        elStart=readPointer+1;
        while(readPointer<rll && headBytes[++readPointer]!=CR && headBytes[++readPointer]!=LF);
        String httpVersion = new String(headBytes,elStart,readPointer-elStart, StandardCharsets.US_ASCII);
        while(headBytes[++readPointer]==CR && headBytes[++readPointer]==LF);
        readPointer--;
        Map<String, String> headers = readHeaders(headBytes,readPointer,rll);

        return new RequestHead(httpVersion,headers,requestMethod,requestTarget);
    }

    private boolean checkHeadDelim(byte[] buffer, int offset, int lim) {
        for(int i=offset;i<lim;i++){
            if(buffer[i]==LF) {
                if(i-1>=0) {
                    if(buffer[i-1]==LF) {
                        return true;
                    }
                    if(buffer[i-1]==CR) {
                        if(i-3>=0 && buffer[i-2]==LF) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Map<String,String> readHeaders(byte[] headBytes, int readPointer, int headSize) {
        Map<String,String> headers = new HashMap<>();
        int elStart;
        int elEnd;
        while(readPointer<headSize) {//todo: reeval this limit
            if(headBytes[readPointer+1]==LF||(headBytes[readPointer+1]==CR&&headBytes[readPointer+2]==LF)) {
                break;
            }
            elStart=readPointer;
            while(headBytes[++readPointer]!=':');
            elEnd=readPointer-1;
            String key = new String(headBytes,elStart,elEnd-elStart, StandardCharsets.US_ASCII);
            readPointer=readPointer+1;
            elStart=readPointer;
            while(headBytes[++readPointer]!=LF);
            if(headBytes[readPointer-1]==CR) {
                elEnd=readPointer-2;
            } else {
                elEnd=readPointer-1;
            }
            String value = new String(headBytes,elStart,elEnd-elStart, StandardCharsets.US_ASCII);
            headers.put(key, value);
        }
        return headers;

    }

}
