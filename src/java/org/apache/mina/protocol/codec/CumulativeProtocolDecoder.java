/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.protocol.codec;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * A {@link ProtocolDecoder} that cumulates the content of received
 * buffers to a <em>cumulative buffer</em> to help users implement decoders.
 * <p>
 * If the received {@link ByteBuffer} is only a part of a message.
 * decoders should cumulate received buffers to make a message complete or
 * to postpone decoding until more buffers arrive.
 * <p>
 * Here is an example decoder that decodes a list of integers:
 * <pre>
 * public class IntegerDecoder extends CumulativeProtocolDecoder {
 * 
 *     public IntegerDecoder() {
 *         super(4);
 *     }
 * 
 *     protected boolean doDecode(ProtocolSession session, ByteBuffer in,
 *                                ProtocolDecoderOutput out) throws ProtocolViolationException {
 *         if (in.remaining() < 4) {
 *             return false; // Cumulate remainder to decode later.
 *         }
 *         
 *         out.write(new Integer(in.getInt()));
 * 
 *         // Decoded one integer; CumulativeProtocolDecoder will call me again,
 *         // so I can decode as many integers as possible.
 *         return true;
 *     }
 * }
 * </pre>
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public abstract class CumulativeProtocolDecoder implements ProtocolDecoder {
    
    /** Cumulation buffer */
    private ByteBuffer buf;
    
    /**
     * Creates a new instance with the specified default capacity of
     * cumulative buffer.  Please note that the capacity increases
     * automatically.
     */
    protected CumulativeProtocolDecoder( int defaultCapacity )
    {
        buf = ByteBuffer.allocate( defaultCapacity );
    }
    
    /**
     * Cumulates content of <tt>in</tt> into internal buffer and forwards
     * decoding request to {@link #doDecode(ProtocolSession, ByteBuffer, ProtocolDecoderOutput)}.
     * <tt>doDecode()</tt> is invoked repeatedly until it returns <tt>false</tt>
     * and the cumulative buffer is compacted after decoding ends.
     * 
     * @throws IllegalStateException if your <tt>doDecode()</tt> returned
     *                               <tt>true</tt> not consuming the cumulative buffer.
     */
    public void decode( ProtocolSession session, ByteBuffer in,
                        ProtocolDecoderOutput out ) throws ProtocolViolationException
    {
        if( session.getTransportType().isStateless() )
        {
            throw new IllegalStateException(
                    "This decoder doesn't work for stateless transport types." );
        }

        put( in );
        
        ByteBuffer buf = this.buf;
        buf.flip();

        try
        {
            for( ;; )
            {
                int oldPos = buf.position();
                if( !doDecode( session, buf, out ) )
                {
                    break;
                }
                
                if( buf.position() == oldPos )
                {
                    throw new IllegalStateException(
                            "doDecode() can't return true when buffer is not consumed." );
                }
            }
        }
        finally
        {
            buf.compact();
        }
    }
    
    /**
     * Implement this method to consume the specified cumulative buffer and
     * decode its content into message(s). 
     *  
     * @param in the cumulative buffer
     * @return <tt>true</tt> if and only if there's more to decode in the buffer
     *         and you want to have <tt>doDecode</tt> method invoked again.
     *         Return <tt>false</tt> if remaining data is not enough to decode,
     *         then this method will be invoked again when more data is cumulated.
     * @throws ProtocolViolationException if cannot decode <tt>in</tt>.
     */
    protected abstract boolean doDecode( ProtocolSession session, ByteBuffer in,
                                         ProtocolDecoderOutput out ) throws ProtocolViolationException;


    private void put( ByteBuffer in )
    {
        ByteBuffer buf = this.buf;
        
        // Check capacity
        if( in.remaining() > buf.remaining() ) {
            int newCapacity = buf.position() + in.remaining();
            ByteBuffer newBuf = ByteBuffer.allocate( newCapacity );
            buf.flip();
            newBuf.put( buf );
            buf.release(); // Release old buffer
            buf = newBuf;
        }
        
        // Copy to cumulative buffer
        buf.put( in.buf() );
        this.buf = buf;
    }
}
