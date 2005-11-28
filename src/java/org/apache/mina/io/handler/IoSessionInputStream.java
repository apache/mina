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

import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;

/**
 * An {@link InputStream} that buffers data read from
 * {@link IoHandler#dataRead(IoSession, ByteBuffer)} events.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
class IoSessionInputStream extends InputStream
{
    private final ByteBuffer buf;
    private boolean closed;
    private boolean released;
    private IOException exception;
    private int waiters;
    
    IoSessionInputStream()
    {
        buf = ByteBuffer.allocate( 16 );
        buf.setAutoExpand( true );
        buf.limit( 0 );
    }

    public synchronized int available()
    {
        if( released )
        {
            return 0;
        }
        else
        {
            return buf.remaining();
        }
    }

    public synchronized void close()
    {
        if( closed )
        {
            return;
        }

        closed = true;
        releaseBuffer();
        
        if( waiters != 0 )
        {
            this.notifyAll();
        }
    }

    public void mark( int readlimit )
    {
    }

    public boolean markSupported()
    {
        return false;
    }

    public synchronized int read() throws IOException
    {
        waitForData();
        if( released )
        {
            return -1;
        }

        int ret = buf.get() & 0xff;
        return ret;
    }

    public synchronized int read( byte[] b, int off, int len ) throws IOException
    {
        waitForData();
        if( released )
        {
            return -1;
        }

        int readBytes;
        if( len > buf.remaining() )
        {
            readBytes = buf.remaining();
        }
        else
        {
            readBytes = len;
        }
        buf.get( b, off, readBytes );
        
        return readBytes;
    }

    public synchronized void reset() throws IOException
    {
        throw new IOException( "Mark is not supported." );
    }

    private void waitForData() throws IOException
    {
        if( released )
        {
            throw new IOException( "Stream is closed." );
        }

        waiters ++;
        while( !released && buf.remaining() == 0 && exception == null )
        {
            try
            {
                this.wait();
            }
            catch( InterruptedException e )
            {
            }
        }
        waiters --;
        
        if( exception != null )
        {
            releaseBuffer();
            throw exception;
        }
        
        if( closed && buf.remaining() == 0 )
        {
            releaseBuffer();
        }
    }
    
    private void releaseBuffer()
    {
        if( released )
        {
            return;
        }

        released = true;
        buf.release();
    }

    synchronized void write( ByteBuffer src )
    {
        if( closed )
        {
            return;
        }
        
        if( buf.hasRemaining() )
        {
            this.buf.compact();
            this.buf.put( src );
            this.buf.flip();
        }
        else
        {
            this.buf.clear();
            this.buf.put( src );
            this.buf.flip();
            this.notify();
        }
    }
    
    synchronized void throwException( IOException e )
    {
        if( exception == null )
        {
            exception = e;
            
            if( waiters != 0 )
            {
                this.notifyAll();
            }
        }
    }
}