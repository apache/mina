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
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.delimited.IoBufferDecoder;

import java.io.IOException;

/**
 *
 */
public class AvroMessageDecoder<GenericRecord> extends IoBufferDecoder<GenericRecord> {

    private Schema schema;

    /**
     * Default Constructor
     * @param schema
     */
    public AvroMessageDecoder(Schema schema) {
        this.schema = schema;
    }

    @Override
    public GenericRecord decode(IoBuffer input) {
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(input.array(), null);
        GenericDatumReader<GenericRecord> recordGenericDatumReader = new GenericDatumReader<GenericRecord>(schema);
        GenericRecord result = null;
        try {
            result = recordGenericDatumReader.read(null, binaryDecoder);
        }catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
        return result;
    }
}
