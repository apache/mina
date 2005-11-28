/*
 *   @(#) $Id: AbstractIoFilterChain.java 330415 2005-11-03 02:19:03Z trustin $
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
package org.apache.mina.io.handler;

import java.io.OutputStream;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.IoSession;

/**
 * An {@link OutputStream} that forwards all write operations to
 * the associated {@link IoSession}.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
class IoSessionOutputStream extends OutputStream
{
    private final IoSession session;
    
    IoSessionOutputStream( IoSession session )
    {
        this.session = session;
    }

    public void close()
    {
        session.close( true );
    }

    public void flush()
    {
    }

    public void write( byte[] b, int off, int len )
    {
        ByteBuffer buf = ByteBuffer.wrap( b, off, len );
        buf.acquire(); // prevent from being pooled.
        session.write( buf, null );
    }

    public void write( byte[] b )
    {
        ByteBuffer buf = ByteBuffer.wrap( b );
        buf.acquire(); // prevent from being pooled.
        session.write( buf, null );
    }

    public void write( int b )
    {
        ByteBuffer buf = ByteBuffer.allocate( 1 );
        buf.put( ( byte ) b );
        buf.flip();
        session.write( buf, null );
    }
}
