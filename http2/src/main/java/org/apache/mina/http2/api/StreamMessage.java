package org.apache.mina.http2.api;

/**
 * Marker interface for messages that are attached to a specific stream.
 * That may not be a start of HTTP PDU (request or response) as they are the
 * one that creates new streams.
 * The use of this interface is not mandatory but not using it will cause
 * request and responses to be pipelined.
 * 
 * @author jeffmaury
 *
 */
public interface StreamMessage {

    /**
     * Return the stream ID the message is attached to.
     * 
     * @return the stream ID
     */
    public int getStreamID();
}
