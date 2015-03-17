/**
 * 
 */
package org.apache.mina.http2.api;

import java.nio.ByteBuffer;

/**
 * @author jeffmaury
 *
 */
public class IntPartialDecoder implements PartialDecoder<Integer> {
    private int size;
    private int remaining;
    private int value;
    
    /**
     * Decode an integer whose size is different from the standard 4.
     * 
     * @param size the size (1,2,3,4) to decode
     */
    public IntPartialDecoder(int size) {
        this.remaining = size;
        this.size = size;
    }

    /**
     * Decode a 4 bytes integer 
     */
    public IntPartialDecoder() {
        this(4);
    }
    
    public boolean consume(ByteBuffer buffer) {
        if (remaining == 0) {
            throw new IllegalStateException();
        }
        while (remaining > 0 && buffer.hasRemaining()) {
            value = (value << 8) + (buffer.get() & 0x00FF);
            --remaining;
        }
        return remaining == 0;
    }
    
    public Integer getValue() {
        if (remaining > 0) {
            throw new IllegalStateException();
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#reset()
     */
    @Override
    public void reset() {
        remaining = size;
        value = 0;
    }
    
    
}
