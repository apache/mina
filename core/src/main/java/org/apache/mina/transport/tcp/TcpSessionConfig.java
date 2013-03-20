/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.transport.tcp;

import java.net.Socket;

import javax.net.ssl.SSLContext;

import org.apache.mina.api.IoSessionConfig;

/**
 *  A {@link IoSessionConfig} for socket based sessions.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface TcpSessionConfig extends IoSessionConfig {
    /**
     * @see Socket#getTcpNoDelay()
     */
    Boolean isTcpNoDelay();

    /**
     * @see Socket#setTcpNoDelay(boolean)
     */
    void setTcpNoDelay(boolean tcpNoDelay);

    /**
     * @see Socket#getKeepAlive() 
     * return <code>null</code> if the default system value is used
     */
    Boolean isKeepAlive();

    /**
     * @see Socket#setKeepAlive(boolean) 
     */
    void setKeepAlive(boolean keepAlive);

    /**
     * @see Socket#getOOBInline() 
     * return <code>null</code> if the default system value is used
     */
    Boolean isOobInline();

    /**
     * @see Socket#setOOBInline(boolean) 
     */
    void setOobInline(boolean oobInline);

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     *
     * @see Socket#getSoLinger()
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     * return <code>null</code> if the default system value is used
     */
    Integer getSoLinger();

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     *
     * @param soLinger Please specify a negative value to disable <tt>SO_LINGER</tt>.
     *
     * @see Socket#setSoLinger(boolean, int)
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     */
    void setSoLinger(int soLinger);

    /**
     * Tells if the session provides some encryption (SSL/TLS)
     * 
     * @return <code>true</code> if the session is secured
     */
    boolean isSecured();

    /**
     * @return The {@link SSLContext} instance stored in the configuration.
     */
    SSLContext getSslContext();

    /**
     * @return The {@link SSLContext} instance stored in the configuration.
     */
    void setSslContext(SSLContext sslContext);
}
