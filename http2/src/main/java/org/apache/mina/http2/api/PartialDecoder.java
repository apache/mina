/**
 * 
 */
package org.apache.mina.http2.api;

import java.nio.ByteBuffer;

/**
 * @author jeffmaury
 *
 */
public interface PartialDecoder<T> {
    /**
     * Consume the buffer so as to decode a value. Not all the input buffer
     * may be consumed.
     * 
     * @param buffer the input buffer to decode
     * @return true if a value is available false if more data is requested
     */
    public boolean consume(ByteBuffer buffer);
    
    /**
     * Return the decoded value.
     * 
     * @return the decoded value
     */
    public T getValue();
    
    /**
     * Reset the internal state of the decoder to that new decoding can take place.
     */
    public void reset();

}
