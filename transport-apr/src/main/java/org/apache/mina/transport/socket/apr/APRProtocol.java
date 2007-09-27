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
package org.apache.mina.transport.socket.apr;

import org.apache.tomcat.jni.Socket;

/**
 * Protocol usable with the {@link APRConnector}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public enum APRProtocol {
    TCP(Socket.APR_PROTO_TCP,Socket.SOCK_STREAM), 
    UDP(Socket.APR_PROTO_UDP,Socket.SOCK_DGRAM),
    SCTP(Socket.APR_PROTO_SCTP,Socket.SOCK_STREAM);

    int socketType;
    int codeProto;

    private APRProtocol(int codeProto,int socketType) {
        this.codeProto = codeProto;
        this.socketType = socketType;
    }
}
