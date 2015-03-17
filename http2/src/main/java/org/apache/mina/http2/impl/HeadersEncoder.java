package org.apache.mina.http2.impl;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.mina.http.api.HttpMessage;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpResponse;
import org.apache.mina.http2.api.Http2Header;

import com.twitter.hpack.Encoder;

import static org.apache.mina.http2.api.Http2Constants.US_ASCII_CHARSET;

public class HeadersEncoder {

    private final Encoder encoder;
    
    public HeadersEncoder(int maxHeaderTableSize) {
        encoder = new Encoder(maxHeaderTableSize);
    }
    
    private static String getMethod(HttpMessage message) {
        String method = null;
        if (message instanceof HttpRequest) {
            ((HttpRequest)message).getMethod().name();
        }
        return method;
    }

    private static String getPath(HttpMessage message) {
        String path = null;
        if (message instanceof HttpRequest) {
            path = ((HttpRequest)message).getTargetURI();
        }
        return path;
    }

    public void encode(HttpMessage message, OutputStream out) throws IOException {
        String value = getMethod(message);
        if (value != null) {
            encoder.encodeHeader(out,
                                 Http2Header.METHOD.getName().getBytes(US_ASCII_CHARSET),
                                 value.getBytes(US_ASCII_CHARSET),
                                 false);
        }
        value = getPath(message);
        if (value != null) {
            encoder.encodeHeader(out,
                                 Http2Header.PATH.getName().getBytes(US_ASCII_CHARSET),
                                 value.getBytes(US_ASCII_CHARSET),
                                 false);
        }
        if (message instanceof HttpResponse) {
            encoder.encodeHeader(out,
                                 Http2Header.STATUS.getName().getBytes(US_ASCII_CHARSET),
                                 Integer.toString(((HttpResponse)message).getStatus().code()).getBytes(US_ASCII_CHARSET),
                                 false);
        }
        for(String name : message.getHeaders().keySet()) {
            if (!Http2Header.isHTTP2ReservedHeader(name)) {
                encoder.encodeHeader(out,
                                     name.getBytes(US_ASCII_CHARSET),
                                     message.getHeaders().get(name).getBytes(US_ASCII_CHARSET),
                                     false);
            }
        }
     }
}
