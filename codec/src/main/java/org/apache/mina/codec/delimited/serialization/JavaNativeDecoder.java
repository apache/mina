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
package org.apache.mina.codec.delimited.serialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.delimited.ByteBufferDecoder;
import org.apache.mina.util.ByteBufferInputStream;

/**
 * Decoder providing the built-in Java-deserialization.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class JavaNativeDecoder<IN extends Serializable> extends ByteBufferDecoder<IN> {

    @SuppressWarnings("unchecked")
    @Override
    public IN decode(final ByteBuffer input) throws ProtocolDecoderException {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteBufferInputStream(input));
            IN s = (IN) ois.readObject();
            ois.close();
            return s;
        } catch (ClassNotFoundException cnfe) {
            throw new ProtocolDecoderException(cnfe);
        } catch (IOException ioe) {
            throw new ProtocolDecoderException(ioe);
        }
    }
}