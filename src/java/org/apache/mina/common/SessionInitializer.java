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

import java.io.IOException;

import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.IoConnector;
import org.apache.mina.protocol.ProtocolAcceptor;
import org.apache.mina.protocol.ProtocolConnector;


/**
 * Initializes session just after it is created.
 * You can adjust {@link SessionConfig} or set pre-define user-defined attributes
 * using this before MINA actually starts communication.
 * <p>
 * Please specify your initializer when you call:
 * <ul>
 *   <li>{@link IoAcceptor}<tt>.bind(...)</tt></li>
 *   <li>{@link IoConnector}<tt>.connect(...)</tt></li>
 *   <li>{@link ProtocolAcceptor}<tt>.bind(...)</tt></li>
 *   <li>{@link ProtocolConnector}<tt>.connect(...)</tt></li>
 * </ul>
 * <p>
 * In case of bind, session is closed immediately and no event is fired if
 * {@link #initializeSession(Session)} throws any exception.  The exception
 * is notified to <tt>ExceptionMonitor</tt>.
 * <p>
 * In case of connect, session is closed immediately and caught exception
 * is forwarded to user application context which called <tt>connect(...)</tt>
 * method.
 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public interface SessionInitializer {

    /**
     * Initializes session just after it is created.
     * You can adjust {@link SessionConfig} or set pre-define user-defined
     * attributes using this before MINA actually starts communication. 
     * Session is closed immediately and no event is fired if
     * {@link #initializeSession(Session)} throws any exception.
     * The exception is notified to <tt>ExceptionMonitor</tt>.
     */
    void initializeSession( Session session ) throws IOException;
}
