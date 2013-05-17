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
package org.apache.mina.codec.delimited;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.generated.protoc.AddressBookProtos.Person;
import org.apache.mina.util.ByteBufferOutputStream;

/**
 * A {@link ProtobufEncoder} and {@link ProtobufDecoder} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProtobufTest extends DelimitTest<Person> {

    @Override
    public List<Person> getObjects() {

        List<Person> list = new LinkedList<Person>();

        list.add(Person.newBuilder().setId(1).setName("Jean Dupond").setEmail("john.white@bigcorp.com").build());
        list.add(Person.newBuilder().setId(2).setName("Marie Blanc").setEmail("marie.blanc@bigcorp.com").build());

        return list;
    }

    @Override
    protected ByteBuffer delimitWithOriginal() throws IOException {
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        bbos.setElastic(true);

        for (Person p : getObjects()) {
            p.writeDelimitedTo(bbos);
        }
        return bbos.getByteBuffer();
    }

    @Override
    public SizePrefixedEncoder<Person> getSerializer() {
        return ProtobufEncoder.newInstance(Person.class);
    }
}
