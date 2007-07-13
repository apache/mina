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
package org.apache.mina.filter.codec.demux;

import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * An abstract {@link MessageDecoder} implementation for those who don't need to
 * implement {@link MessageDecoder#finishDecode(IoSession, ProtocolDecoderOutput)}
 * method.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 */
public abstract class MessageDecoderAdapter implements MessageDecoder {
    /**
     * Override this method to deal with the closed connection.
     * The default implementation does nothing.
     */
    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception {
    }
}
