/**
 * 
 */
package org.apache.mina.http2.api;

import java.nio.ByteBuffer;

/**
 * @author jeffmaury
 *
 */
public class LongPartialDecoder implements PartialDecoder<Long> {
    private int size;
    private int remaining;
    private long value;
    
    /**
     * Decode a long integer whose size is different from the standard 8.
     * 
     * @param size the size (1 to 8) to decode
     */
    public LongPartialDecoder(int size) {
        this.remaining = size;
        this.size = size;
    }

    /**
     * Decode a 8 bytes long integer 
     */
    public LongPartialDecoder() {
        this(8);
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
    
    public Long getValue() {
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
