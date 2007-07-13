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
package org.apache.mina.filter.codec;

/**
 * An exception that is thrown when {@link ProtocolEncoder}
 * cannot understand or failed to validate the specified message object.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolEncoderException extends ProtocolCodecException {
    private static final long serialVersionUID = 8752989973624459604L;

    /**
     * Constructs a new instance.
     */
    public ProtocolEncoderException() {
    }

    /**
     * Constructs a new instance with the specified message.
     */
    public ProtocolEncoderException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance with the specified cause.
     */
    public ProtocolEncoderException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new instance with the specified message and the specified
     * cause.
     */
    public ProtocolEncoderException(String message, Throwable cause) {
        super(message, cause);
    }
}