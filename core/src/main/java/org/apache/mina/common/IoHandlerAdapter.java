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
package org.apache.mina.common;

import org.apache.mina.util.SessionLog;
import org.apache.mina.util.SessionUtil;

/**
 * An abstract adapter class for {@link IoHandler}.  You can extend this
 * class and selectively override required event handler methods only.  All
 * methods do nothing by default. 
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoHandlerAdapter implements IoHandler {
    public void sessionCreated(IoSession session) throws Exception {
        SessionUtil.initialize(session);
    }

    public void sessionOpened(IoSession session) throws Exception {
    }

    public void sessionClosed(IoSession session) throws Exception {
    }

    public void sessionIdle(IoSession session, IdleStatus status)
            throws Exception {
    }

    public void exceptionCaught(IoSession session, Throwable cause)
            throws Exception {
        if (SessionLog.isWarnEnabled(session)) {
            SessionLog.warn(session, "EXCEPTION, please implement "
                    + getClass().getName()
                    + ".exceptionCaught() for proper handling:", cause);
        }
    }

    public void messageReceived(IoSession session, Object message)
            throws Exception {
    }

    public void messageSent(IoSession session, Object message) throws Exception {
    }
}