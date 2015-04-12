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
package org.apache.mina.http2.api;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_SETTINGS;
/**
 * An HTTP2 SETTINGS frame.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2SettingsFrame extends Http2Frame {
    private final Collection<Http2Setting> settings;
    
    public Collection<Http2Setting> getSettings() {
        return settings;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        for(Http2Setting setting : getSettings()) {
            buffer.putShort((short) setting.getID());
            buffer.putInt((int) setting.getValue());
        }
    }

    protected Http2SettingsFrame(Http2SettingsFrameBuilder builder) {
        super(builder);
        this.settings = builder.getSettings();
    }

    public static class Http2SettingsFrameBuilder extends AbstractHttp2FrameBuilder<Http2SettingsFrameBuilder,Http2SettingsFrame> {
        private Collection<Http2Setting> settings = Collections.emptyList();
        
        public Http2SettingsFrameBuilder settings(Collection<Http2Setting> settings) {
            this.settings = settings;
            return this;
        }
        
        public Collection<Http2Setting> getSettings() {
            return settings;
        }

        @Override
        public Http2SettingsFrame build() {
            if (getLength() == (-1)) {
                setLength(getSettings().size() * 6);
            }
            return new Http2SettingsFrame(type(FRAME_TYPE_SETTINGS));
        }
        
        public static Http2SettingsFrameBuilder builder() {
            return new Http2SettingsFrameBuilder();
        }
    }
}
