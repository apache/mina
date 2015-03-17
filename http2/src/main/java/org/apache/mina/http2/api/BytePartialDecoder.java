/**
 * 
 */
package org.apache.mina.http2.api;

import java.nio.ByteBuffer;

/**
 * @author jeffmaury
 *
 */
public class BytePartialDecoder implements PartialDecoder<byte[]> {
    private int offset;
    private byte[] value;
    
    /**
     * Decode an byte array.
     * 
     * @param size the size of the byte array to decode
     */
    public BytePartialDecoder(int size) {
        this.offset = 0;
        this.value = new byte[size];
    }

    public boolean consume(ByteBuffer buffer) {
        if (value.length - offset == 0) {
            throw new IllegalStateException();
        }
        int length = Math.min(buffer.remaining(), value.length - offset);
        buffer.get(value, offset, length);
        offset += length;
        return value.length - offset == 0;
    }
    
    public byte[] getValue() {
        if (value.length - offset > 0) {
            throw new IllegalStateException();
        }
        return value;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        offset = 0;
    }
}
