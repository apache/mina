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

import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.mina.codec.ProtocolEncoderException;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Avro Message encoder
 *
 * It can be used to handle both a Generic Record as well as Specific Record
 */
public class AvroMessageEncoder<T extends GenericContainer> extends ByteBufferEncoder<T> {

    // Logger
    public static final Logger LOG = LoggerFactory.getLogger(AvroMessageEncoder.class);

    private ByteBuffer encodedMessage;

    @Override
    public int getEncodedSize(T message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Need to check for writer
        if(message instanceof GenericRecord) {
            DatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(message.getSchema());
            Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            try {
                writer.write((GenericRecord)message, encoder);
                encoder.flush();
                byte[] encoded = out.toByteArray();
                encodedMessage = ByteBuffer.wrap(encoded);
                out.close();
            } catch (IOException ioEx) {
                LOG.error("error while marshalling", ioEx);
                throw new ProtocolEncoderException(ioEx.getMessage());
            }
        } else if (message instanceof SpecificRecord) {
            DatumWriter<T> writer = new SpecificDatumWriter<T>(message.getSchema());
            Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            try {
                writer.write(message, encoder);
                encoder.flush();
                byte[] encoded = out.toByteArray();
                encodedMessage = ByteBuffer.wrap(encoded);
                out.close();
            } catch (IOException ioEx) {
                LOG.error("error while marshalling", ioEx);
                throw new ProtocolEncoderException(ioEx.getMessage());
            }
        } else {
            LOG.warn("Unknown object type, serialization method not known for {}", message.getClass());
            throw new ProtocolEncoderException(message.getClass() + " cannot be Serialized");
        }

        return encodedMessage != null ? encodedMessage.capacity() : -1;
    }

    @Override
    public void writeTo(T message, ByteBuffer buffer) {
        buffer.put(encodedMessage);
    }
}
