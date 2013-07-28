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
package org.apache.mina.avro.codec;

import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericRecord;
import org.apache.mina.avro.codec.serialization.AvroMessageEncoder;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.codec.delimited.SizePrefixedEncoder;
import org.apache.mina.codec.delimited.ints.VarInt;

/**
 * Avro Encoder
 *
 * It used GenericContainer, parent class for Avro's GenericRecord and SpecificRecord
 * User need to specify the type while creating the instance of the Encoder. The default
 * Size encoder is {@code VarInt} encoder
 */
public class AvroEncoder<T extends GenericContainer> extends SizePrefixedEncoder<T> {

    /**
     * Intializes the Avro Encoder
     *
     * @param sizeEncoder       Size Prefix encoder
     * @param payloadEncoder    Avro Encoder to encode message into Avro format
     */
    public AvroEncoder(ByteBufferEncoder<Integer> sizeEncoder, ByteBufferEncoder<T> payloadEncoder) {
        super(sizeEncoder, payloadEncoder);
    }

    /**
     * Intializes the Avro Encoder
     * Default Encoder are VarInt encoder for Size and {@link AvroEncoder} for Avro encoding
     */
    public AvroEncoder() {
        super(new VarInt().getEncoder(), new AvroMessageEncoder<T>());
    }
}
