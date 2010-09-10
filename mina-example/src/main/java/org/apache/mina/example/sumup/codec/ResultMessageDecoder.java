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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.example.sumup.message.AbstractMessage;
import org.apache.mina.example.sumup.message.ResultMessage;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;

/**
 * A {@link MessageDecoder} that decodes {@link ResultMessage}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ResultMessageDecoder extends AbstractMessageDecoder {
    private int code;

    private boolean readCode;

    public ResultMessageDecoder() {
        super(Constants.RESULT);
    }

    @Override
    protected AbstractMessage decodeBody(IoSession session, IoBuffer in) {
        if (!readCode) {
            if (in.remaining() < Constants.RESULT_CODE_LEN) {
                return null; // Need more data.
            }

            code = in.getShort();
            readCode = true;
        }

        if (code == Constants.RESULT_OK) {
            if (in.remaining() < Constants.RESULT_VALUE_LEN) {
                return null;
            }

            ResultMessage m = new ResultMessage();
            m.setOk(true);
            m.setValue(in.getInt());
            readCode = false;
            return m;
        } else {
            ResultMessage m = new ResultMessage();
            m.setOk(false);
            readCode = false;
            return m;
        }
    }

    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception {
    }
}
