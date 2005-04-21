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
package org.apache.mina.io;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.Session;

/**
 * A {@link Session} that represents low-level I/O connection between two
 * endpoints regardless of underlying transport types.
 *   
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see Session
 */
public interface IoSession extends Session
{
    /**
     * Returns the event handler for this session.
     */
    IoHandler getHandler();

    /**
     * Writes the content of the specified <code>buf</code>.
     * This operation is asynchronous, and you'll get notified by
     * {@link IoHandler#dataWritten(IoSession, Object)} event.
     * The specified <code>marker</code> will be passes as a parameter.
     */
    void write( ByteBuffer buf, Object marker );
}