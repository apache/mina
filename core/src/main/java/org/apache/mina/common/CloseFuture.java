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
 * An {@link IoFuture} for asynchronous close requests.
 *
 * <h3>Example</h3>
 * <pre>
 * IoSession session = ...;
 * CloseFuture future = session.close();
 * // Wait until the connection is closed
 * future.join();
 * // Now connection should be closed.
 * assert future.isClosed();
 * </pre>
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class CloseFuture extends IoFuture
{
    /**
     * Creates a new instance.
     */
    public CloseFuture()
    {
    }
    
    /**
     * Creates a new instance which uses the specified object as a lock.
     */
    public CloseFuture( Object lock )
    {
        super( lock );
    }
    
    /**
     * Returns <tt>true</tt> if the close request is finished and the session is closed.
     */
    public boolean isClosed()
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
    public void setClosed()
    {
        setValue( Boolean.TRUE );
    }
}
