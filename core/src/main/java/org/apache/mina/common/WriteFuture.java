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
package org.apache.mina.common;

/**
 * An {@link IoFuture} for asynchronous write requests.
 *
 * <h3>Example</h3>
 * <pre>
 * IoSession session = ...;
 * WriteFuture future = session.write(...);
 * // Wait until the message is completely written out to the O/S buffer.
 * future.join();
 * if( future.isWritten() )
 * {
 *     // The message has been written successfully.
 * }
 * else
 * {
 *     // The messsage couldn't be written out completely for some reason.
 *     // (e.g. Connection is closed)
 * }
 * </pre>
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class WriteFuture extends IoFuture
{
    /**
     * Returns a new {@link WriteFuture} which is already marked as 'written'.
     */
    public static WriteFuture newWrittenFuture()
    {
        WriteFuture unwrittenFuture = new WriteFuture();
        unwrittenFuture.setWritten( true );
        return unwrittenFuture;
    }

    /**
     * Returns a new {@link WriteFuture} which is already marked as 'not written'.
     */
    public static WriteFuture newNotWrittenFuture()
    {
        WriteFuture unwrittenFuture = new WriteFuture();
        unwrittenFuture.setWritten( false );
        return unwrittenFuture;
    }
    
    /**
     * Creates a new instance.
     */
    public WriteFuture()
    {
    }
    
    /**
     * Creates a new instance which uses the specified object as a lock.
     */
    public WriteFuture( Object lock )
    {
        super( lock );
    }
    
    /**
     * Returns <tt>true</tt> if the write operation is finished successfully.
     */
    public boolean isWritten()
    {
        if( isReady() )
        {
            return ( ( Boolean ) getValue() ).booleanValue();
        }
        else
        {
            return false;
        }
    }

    /**
     * This method is invoked by MINA internally.  Please do not call this method
     * directly.
     */
    public void setWritten( boolean written )
    {
        setValue( written? Boolean.TRUE : Boolean.FALSE );
    }
}
