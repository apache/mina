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

package org.apache.mina.avro.codec.serialization;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.delimited.IoBufferDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Avro Message Decoder
 *
 * Uses ReflectDatumReader to read the data from the stream
 */
public class AvroMessageDecoder<T extends GenericContainer> extends IoBufferDecoder<T> {

    // Logger
    public static final Logger LOG = LoggerFactory.getLogger(AvroMessageDecoder.class);

    // Avro Schema used for decoding
    private Schema schema;

    /**
     * Default Constructor
     * @param schema    Avro Schema to be used for decoding the messages
     */
    public AvroMessageDecoder(Schema schema) {
        if(schema == null) {
            LOG.error("Avro Schema cannot be null");
            throw new IllegalArgumentException("Avro Schema cannot be null");
        }
        this.schema = schema;
    }

    @Override
    public T decode(IoBuffer input) {
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(input.array(), null);
        ReflectDatumReader<T> reader = new ReflectDatumReader<T>(schema);
        T result = null;
        try {
            result = reader.read(null, binaryDecoder);
        }catch (IOException ioEx) {
            LOG.error("Error while decoding", ioEx);
            throw new ProtocolDecoderException(ioEx.getMessage());
        }
        return result;
    }
}
