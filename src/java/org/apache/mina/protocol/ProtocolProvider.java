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
package org.apache.mina.protocol;

import java.net.SocketAddress;

/**
 * Provides a required information to implement high-level protocols.
 * It consists of:
 * <ul>
 *   <li>{@link ProtocolCodecFactory} - provides {@link ProtocolEncoder} and
 *       {@link ProtocolDecoder} which translates binary or protocol specific
 *       data into message object and vice versa.</li>
 *   <li>{@link ProtocolHandler} - handles high-leve protocol events.</li>
 * </ul>
 * <p>
 * If once you implement {@link ProtocolProvider} for your protocol, you can
 * connect to or bind on {@link SocketAddress} using {@link ProtocolAcceptor}
 * and {@link ProtocolConnector}.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/ReverseProtocolProvider.html"><code>ReverserProtocolProvider</code></a>
 * example. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public interface ProtocolProvider
{
    ProtocolCodecFactory getCodecFactory();

    ProtocolHandler getHandler();
}