package org.apache.mina.http2.api;

public class Http2Setting {
    private int ID;

    private long value;

    public int getID() {
        return ID;
    }

    public void setID(int iD) {
        ID = iD;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
