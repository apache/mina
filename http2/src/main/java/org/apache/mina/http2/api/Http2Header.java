package org.apache.mina.http2.api;

public enum Http2Header {

    METHOD(":method"),
    
    PATH(":path"),
    
    STATUS(":status"),
    
    AUTHORITY(":authority"),
    
    SCHEME(":scheme");
    
    private final String name;
    
    private Http2Header(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Check whether a header is an HTTP2 reserved one.
     * 
     * @param name the name of the HTTP header
     * @return true is this is a reserved HTTP2 header, false otherwise
     */
    public static boolean isHTTP2ReservedHeader(String name) {
        for(Http2Header header : Http2Header.values()) {
            if (header.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
