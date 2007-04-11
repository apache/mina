package org.apache.mina.common;

import java.net.SocketAddress;

/**
 * A wrapper for an existing {@link WriteRequest}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class WriteRequestWrapper implements WriteRequest {

    private final WriteRequest writeRequest;
    
    /**
     * Creates a new instance that wraps the specified request.
     */
    public WriteRequestWrapper(WriteRequest writeRequest) {
        if (writeRequest == null) {
            throw new NullPointerException("writeRequest");
        }
        this.writeRequest = writeRequest;
    }
    
    public SocketAddress getDestination() {
        return writeRequest.getDestination();
    }

    public WriteFuture getFuture() {
        return writeRequest.getFuture();
    }

    public Object getMessage() {
        return writeRequest.getMessage();
    }
    
    /**
     * Returns the wrapped request object.
     */
    public WriteRequest getWriteRequest() {
       return writeRequest; 
    }
    
    @Override
    public String toString() {
        return getMessage().toString();
    }
}
