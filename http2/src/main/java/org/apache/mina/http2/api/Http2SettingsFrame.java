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

import java.util.Collection;

/**
 * An SPY data frame
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2SettingsFrame extends Http2Frame {
    private final Collection<Http2Setting> settings;
    
    public Collection<Http2Setting> getSettings() {
        return settings;
    }

    protected <T extends AbstractHttp2SettingsFrameBuilder<T,V>, V extends Http2SettingsFrame> Http2SettingsFrame(AbstractHttp2SettingsFrameBuilder<T, V> builder) {
        super(builder);
        this.settings = builder.getSettings();
    }

    
    public static abstract class AbstractHttp2SettingsFrameBuilder<T extends AbstractHttp2SettingsFrameBuilder<T,V>, V extends Http2SettingsFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private Collection<Http2Setting> settings;
        
        @SuppressWarnings("unchecked")
        public T settings(Collection<Http2Setting> settings) {
            this.settings = settings;
            return (T) this;
        }
        
        public Collection<Http2Setting> getSettings() {
            return settings;
        }
    }
    
    public static class Builder extends AbstractHttp2SettingsFrameBuilder<Builder, Http2SettingsFrame> {

        @Override
        public Http2SettingsFrame build() {
            return new Http2SettingsFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
