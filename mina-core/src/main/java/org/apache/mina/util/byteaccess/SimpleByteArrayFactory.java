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
package org.apache.mina.util.byteaccess;


import org.apache.mina.core.buffer.IoBuffer;


/**
 * Creates <code>ByteArray</code> backed by a heap-allocated
 * <code>IoBuffer</code>. The free method on returned
 * <code>ByteArray</code>s is a nop.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SimpleByteArrayFactory implements ByteArrayFactory
{
    /**
     * 
     * Creates a new instance of SimpleByteArrayFactory.
     *
     */
    public SimpleByteArrayFactory()
    {
        super();
    }


    /**
     * @inheritDoc
     */
    public ByteArray create( int size )
    {
        if ( size < 0 )
        {
            throw new IllegalArgumentException( "Buffer size must not be negative:" + size );
        }
        IoBuffer bb = IoBuffer.allocate( size );
        ByteArray ba = new BufferByteArray( bb )
        {

            @Override
            public void free()
            {
                // Nothing to do.
            }

        };
        return ba;
    }

}
