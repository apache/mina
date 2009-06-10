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
package org.apache.mina.example.tapedeck;

import org.apache.mina.filter.codec.ProtocolDecoderException;

/**
 * Exception thrown by {@link CommandDecoder} when a line cannot be decoded as 
 * a {@link Command} object.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CommandSyntaxException extends ProtocolDecoderException {
    private static final long serialVersionUID = 4903547501059093765L;

    private final String message;
    
    public CommandSyntaxException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
