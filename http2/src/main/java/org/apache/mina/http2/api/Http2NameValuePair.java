package org.apache.mina.http2.api;

public class Http2NameValuePair {
    private String name;
    private String value;
    
    /**
     * Build a name/value pair given the name and value.
     * 
     * @param name the name of the pair
     * @param value the value of the pair
     */
    public Http2NameValuePair(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    
    
}
