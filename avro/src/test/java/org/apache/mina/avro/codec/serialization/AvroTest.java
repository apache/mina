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
import org.apache.avro.generic.GenericRecord;
import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.codec.delimited.IoBufferDecoder;
import org.apache.mina.codec.delimited.serialization.GenericSerializerTest;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AvroTest extends GenericSerializerTest {

    private static Schema SCHEMA;

    static {
        try {
            SCHEMA = new Schema.Parser().parse(AvroTest.class.getClassLoader().getResourceAsStream("user.avsc"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IoBufferDecoder getDecoder() throws Exception {
        return new AvroMessageDecoder<GenericRecord>(SCHEMA);
    }

    @Override
    public ByteBufferEncoder getEncoder() throws Exception {
        return new AvroMessageEncoder<GenericRecord>();
    }

    @Override
    public List<GenericRecord> getObjects() {
        List<GenericRecord> genericRecordList = new ArrayList<GenericRecord>(1);
        GenericRecord record1 = new GenericData.Record(SCHEMA);
        record1.put("name", "Black Jack");
        record1.put("favorite_number", 11);
        record1.put("favorite_color", "Black");
        genericRecordList.add(record1);

        return genericRecordList;
    }

    @Test
    public void testMessage() throws Exception {
        ByteBufferEncoder<GenericRecord> encoder = getEncoder();
        AvroMessageDecoder<GenericRecord> decoder = new AvroMessageDecoder<GenericRecord>(SCHEMA);

        for (GenericRecord object : getObjects()) {
            GenericRecord message = decoder.decode(IoBuffer
                    .wrap(encoder.encode(object)));
            System.out.println(message);
        }
    }
}
