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
package org.apache.mina.example.sumup.codec;

import org.apache.mina.example.sumup.message.AddMessage;
import org.apache.mina.example.sumup.message.ResultMessage;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;

/**
 * A {@link ProtocolCodecFactory} that provides a protocol codec for
 * SumUp protocol.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SumUpProtocolCodecFactory extends DemuxingProtocolCodecFactory {

    public SumUpProtocolCodecFactory(boolean server) {
        if (server) {
            super.addMessageDecoder(AddMessageDecoder.class);
            super.addMessageEncoder(ResultMessage.class, ResultMessageEncoder.class);
        } else // Client
        {
            super.addMessageEncoder(AddMessage.class, AddMessageEncoder.class);
            super.addMessageDecoder(ResultMessageDecoder.class);
        }
    }
}
