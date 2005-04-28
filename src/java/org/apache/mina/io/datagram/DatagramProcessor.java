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
package org.apache.mina.io.datagram;

import org.apache.mina.common.BaseSessionManager;

/**
 * A base class for {@link DatagramAcceptor} and {@link DatagramConnector}.
 * Session interacts with this abstract class instead of those two concrete
 * classes.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
abstract class DatagramProcessor extends BaseSessionManager
{
    /**
     * Requests this processor to flush the write buffer of the specified
     * session.  This method is invoked by MINA internally.
     */
    abstract void flushSession( DatagramSession session );

    /**
     * Requests this processor to close the specified session.
     * This method is invoked by MINA internally.
     */
    abstract void closeSession( DatagramSession session );
}