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

import org.apache.mina.codec.delimited.ints.VarInt;
import org.apache.mina.codec.delimited.serialization.ThriftMessageEncoder;
import org.apache.thrift.TBase;

public class ThriftEncoder<OUT extends TBase<?, ?>> extends SizePrefixedEncoder<OUT> {

    static public <L extends TBase<?, ?>> ThriftEncoder<L> newInstance(Class<L> clazz) throws SecurityException,
            NoSuchMethodException {
        return new ThriftEncoder<L>(clazz);
    }

    public ThriftEncoder(Class<OUT> clazz) throws SecurityException, NoSuchMethodException {
        super(new VarInt.Encoder(), ThriftMessageEncoder.newInstance(clazz));
    }
}
