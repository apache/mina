/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.examples.reverser;

import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolProvider;

/**
 * {@link ProtocolProvider} implementation for reverser server protocol.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class ReverseProtocolProvider implements ProtocolProvider
{

    // Protocol handler is usually a singleton.
    private static ProtocolHandler HANDLER = new ReverseProtocolHandler();

    // Codec factory is also usually a singleton.
    private static ProtocolCodecFactory CODEC_FACTORY = new ProtocolCodecFactory()
    {
        public ProtocolEncoder newEncoder()
        {
            // Create a new encoder.
            return new TextLineEncoder();
        }

        public ProtocolDecoder newDecoder()
        {
            // Create a new decoder.
            return new TextLineDecoder();
        }
    };

    public ProtocolCodecFactory getCodecFactory()
    {
        return CODEC_FACTORY;
    }

    public ProtocolHandler getHandler()
    {
        return HANDLER;
    }

}