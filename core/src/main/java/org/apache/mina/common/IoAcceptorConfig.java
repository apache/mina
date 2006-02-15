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
 * A configuration which is used to configure {@link IoAcceptor}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoAcceptorConfig extends IoServiceConfig
{
    /**
     * Returns <tt>true</tt> if and only if all clients are disconnected
     * when this acceptor unbinds the related local address.
     */
    boolean isDisconnectOnUnbind();
    
    /**
     * Sets whether all clients are disconnected when this acceptor unbinds the
     * related local address.  The default value is <tt>true</tt>.
     */
    void setDisconnectOnUnbind( boolean disconnectOnUnbind );
}
