/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.filter.codec.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * A {@link ProtocolEncoderOutput} based on queue.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class SimpleProtocolEncoderOutput implements ProtocolEncoderOutput
{
    private final List<ByteBuffer> bufferQueue = new ArrayList<ByteBuffer>();

    public SimpleProtocolEncoderOutput()
    {
    }

    public List<ByteBuffer> getBufferQueue()
    {
        return bufferQueue;
    }

    public void write( ByteBuffer buf )
    {
        bufferQueue.add( buf );
    }

    public void mergeAll()
    {
        final int size = bufferQueue.size();

        if( size < 2 )
        {
            // no need to merge!
            return;
        }

        // Get the size of merged BB
        int sum = 0;
        for( int i = size - 1; i >= 0; i -- )
        {
            sum += bufferQueue.get( i ).remaining();
        }

        // Allocate a new BB that will contain all fragments
        ByteBuffer newBuf = ByteBuffer.allocate( sum );

        // and merge all.
        for( ; !bufferQueue.isEmpty(); )
        {
            ByteBuffer buf = bufferQueue.remove( 0 );

            newBuf.put( buf );
            buf.release();
        }

        // Push the new buffer finally.
        newBuf.flip();
        bufferQueue.add( newBuf );
    }

    public WriteFuture flush()
    {
        WriteFuture future = null;

        for( ; !bufferQueue.isEmpty(); )
        {
            ByteBuffer buf = bufferQueue.remove( 0 );

            // Flush only when the buffer has remaining.
            if( buf.hasRemaining() )
            {
                future = doFlush( buf );
            }
        }

        return future;
    }

    protected abstract WriteFuture doFlush( ByteBuffer buf );
}