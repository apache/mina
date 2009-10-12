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
package org.apache.mina.transport.serial;

import org.apache.mina.core.session.IoSession;

/**
 * An {@link IoSession} for serial communication transport.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface SerialSession extends IoSession {
    SerialSessionConfig getConfig();

    SerialAddress getRemoteAddress();

    SerialAddress getLocalAddress();

    SerialAddress getServiceAddress();
    
    /**
     * Sets or clears the RTS (Request To Send) bit in the UART, if supported by the underlying implementation.
     * @param rts true for set RTS, false for clearing
     */
    void setRTS(boolean rts);

    /**
     * Gets the state of the RTS (Request To Send) bit in the UART, if supported by the underlying implementation. 
     */
    boolean isRTS();

    /**
     * Sets or clears the DTR (Data Terminal Ready) bit in the UART, if supported by the underlying implementation.
     * @param dtr true for set DTR, false for clearing
     */
    void setDTR(boolean dtr);

    /**
     * Gets the state of the DTR (Data Terminal Ready) bit in the UART, if supported by the underlying implementation. 
     */
    boolean isDTR();
}
