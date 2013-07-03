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

import java.nio.ByteOrder;

import org.apache.mina.codec.delimited.ints.RawInt32;
import org.apache.mina.codec.delimited.serialization.ThriftMessageDecoder;
import org.apache.thrift.TBase;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ThriftDecoder<M extends TBase<?, ?>> extends SizePrefixedDecoder<M> {
    public static <L extends TBase<?, ?>> ThriftDecoder<L> newInstance(Class<L> clazz) throws NoSuchMethodException {
        return new ThriftDecoder<L>(clazz);
    }

    public ThriftDecoder(Class<M> clazz) throws NoSuchMethodException {
        super(new RawInt32(ByteOrder.BIG_ENDIAN).getDecoder(), new ThriftMessageDecoder<>(clazz));
    }
}
