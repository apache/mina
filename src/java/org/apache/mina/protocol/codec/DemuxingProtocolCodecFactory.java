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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * A composite {@link ProtocolCodecFactory} that consists of multiple
 * {@link MessageEncoder}s and {@link MessageDecoder}s.
 * {@link ProtocolEncoder} and {@link ProtocolDecoder} this factory
 * returns demultiplex incoming messages and buffers to
 * appropriate {@link MessageEncoder}s and {@link MessageDecoder}s. 
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see MessageEncoder
 * @see MessageDecoder
 */
public class DemuxingProtocolCodecFactory implements ProtocolCodecFactory {

    private MessageDecoder[] decoders = new MessageDecoder[0];
    private final Map encoders = new HashMap();
    
    private final ProtocolEncoder protocolEncoder = new ProtocolEncoderImpl();

    private final ProtocolDecoder protocolDecoder = new ProtocolDecoderImpl();
    
    public DemuxingProtocolCodecFactory()
    {
    }
    
    public void register( MessageEncoder encoder )
    {
        Iterator it = encoder.getMessageTypes().iterator();
        while( it.hasNext() )
        {
            Class type = (Class) it.next();
            encoders.put( type, encoder );
        }
    }
    
    public void register( MessageDecoder decoder )
    {
        if( decoder == null )
        {
            throw new NullPointerException( "decoder" );
        }
        MessageDecoder[] decoders = this.decoders;
        MessageDecoder[] newDecoders = new MessageDecoder[ decoders.length + 1 ];
        System.arraycopy( decoders, 0, newDecoders, 0, decoders.length );
        newDecoders[ decoders.length ] = decoder;
        this.decoders = newDecoders;
    }
    
    public ProtocolEncoder newEncoder() {
        return protocolEncoder;
    }

    public ProtocolDecoder newDecoder() {
        return protocolDecoder;
    }
    
    private class ProtocolEncoderImpl implements ProtocolEncoder
    {
        public void encode( ProtocolSession session, Object message,
                            ProtocolEncoderOutput out ) throws ProtocolViolationException
        {
            Class type = message.getClass();
            MessageEncoder encoder = findEncoder( type );
            if( encoder == null )
            {
                throw new ProtocolViolationException( "Unexpected message type: " + type );
            }
            
            encoder.encode( session, message, out );
        }
        
        private MessageEncoder findEncoder( Class type )
        {
            MessageEncoder encoder = ( MessageEncoder ) encoders.get( type );
            if( encoder == null )
            {
                encoder = findEncoder( type, new HashSet() );
            }

            return encoder;
        }

        private MessageEncoder findEncoder( Class type, Set triedClasses )
        {
            MessageEncoder encoder;

            if( triedClasses.contains( type ) )
                return null;
            triedClasses.add( type );

            encoder = ( MessageEncoder ) encoders.get( type );
            if( encoder == null )
            {
                encoder = findEncoder( type, triedClasses );
                if( encoder != null )
                    return encoder;

                Class[] interfaces = type.getInterfaces();
                for( int i = 0; i < interfaces.length; i ++ )
                {
                    encoder = findEncoder( interfaces[ i ], triedClasses );
                    if( encoder != null )
                        return encoder;
                }

                return null;
            }
            else
                return encoder;
        }
    }
    
    private class ProtocolDecoderImpl extends CumulativeProtocolDecoder
    {
        private MessageDecoder currentDecoder;

        protected ProtocolDecoderImpl()
        {
            super( 16 );
        }

        protected boolean doDecode( ProtocolSession session, ByteBuffer in,
                                    ProtocolDecoderOutput out) throws ProtocolViolationException
        {
            if( currentDecoder == null )
            {
                MessageDecoder[] decoders = DemuxingProtocolCodecFactory.this.decoders;
                int undecodables = 0;
                for( int i = decoders.length - 1; i >= 0; i -- ) 
                {
                    MessageDecoder decoder = decoders[i];
                    int limit = in.limit();
                    in.position( 0 );
                    MessageDecoderResult result = decoder.decodable( session, in );
                    in.position( 0 );
                    in.limit( limit );
                    
                    if( result == MessageDecoder.OK )
                    {
                        currentDecoder = decoder;
                        break;
                    }
                    else if( result == MessageDecoder.NOT_OK )
                    {
                        undecodables ++;
                    }
                    else if( result != MessageDecoder.NEED_DATA )
                    {
                        throw new IllegalStateException( "Unexpected decode result (see your decodable()): " + result );
                    }
                }
                
                if( undecodables == decoders.length )
                {
                    // Throw an exception if all decoders cannot decode data.
                    in.position( in.limit() ); // Skip data
                    throw new ProtocolViolationException(
                            "No appropriate message decoder: " + in.getHexDump() );
                }
                
                if( currentDecoder == null )
                {
                    // Decoder is not determined yet (i.e. we need more data)
                    return false;
                }
            }
            
            MessageDecoderResult result = currentDecoder.decode( session, in, out );
            if( result == MessageDecoder.OK )
            {
                currentDecoder = null;
                return true;
            }
            else if( result == MessageDecoder.NEED_DATA )
            {
                return false;
            }
            else if( result == MessageDecoder.NOT_OK )
            {
                throw new ProtocolViolationException( "Message decoder returned NOT_OK." );
            }
            else
            {
                throw new IllegalStateException( "Unexpected decode result (see your decode()): " + result );
            }
        }
    }
}
