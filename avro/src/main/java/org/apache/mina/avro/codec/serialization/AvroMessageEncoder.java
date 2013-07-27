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

import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.mina.codec.delimited.ByteBufferEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 */
public class AvroMessageEncoder<OUT extends GenericRecord> extends ByteBufferEncoder<GenericRecord> {

    private ByteBuffer encodedMessage;

    @Override
    public int getEncodedSize(GenericRecord message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(message.getSchema());
        Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        try {
            writer.write(message, encoder);
            encoder.flush();
            byte[] encoded = out.toByteArray();
            encodedMessage = ByteBuffer.wrap(encoded);
            out.close();
        } catch (IOException ioEx) {
            // :(
        }
        return encodedMessage != null ? encodedMessage.capacity() : -1;
    }

    @Override
    public void writeTo(GenericRecord message, ByteBuffer buffer) {
        buffer.put(encodedMessage);
    }
}
