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
package org.apache.mina.filter.codec.asn1;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.stateful.DecoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.StatefulDecoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Adapts {@link StatefulDecoder} to MINA <tt>ProtocolDecoder</tt>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$, 
 */
public class Asn1CodecDecoder implements ProtocolDecoder
{

    private final StatefulDecoder decoder;

    private final DecoderCallbackImpl callback = new DecoderCallbackImpl();

    public Asn1CodecDecoder( StatefulDecoder decoder )
    {
        decoder.setCallback( callback );
        this.decoder = decoder;
    }

    public void decode( IoSession session, ByteBuffer in,
                        ProtocolDecoderOutput out ) throws DecoderException
    {
        callback.decOut = out;
        decoder.decode( in.buf() );
    }

    public void dispose( IoSession session ) throws Exception
    {
    }

    private class DecoderCallbackImpl implements DecoderCallback
    {
        private ProtocolDecoderOutput decOut;

        public void decodeOccurred( StatefulDecoder decoder, Object decoded )
        {
            decOut.write( decoded );
        }
    }
}
