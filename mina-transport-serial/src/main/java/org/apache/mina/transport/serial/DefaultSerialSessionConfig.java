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
package org.apache.mina.transport.serial;

import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.core.session.IoSessionConfig;

/**
 * The default configuration for a serial session {@link SerialSessionConfig}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class DefaultSerialSessionConfig extends AbstractIoSessionConfig implements SerialSessionConfig {

    private int receiveThreshold = -1;

    private int inputBufferSize = 8;

    private int outputBufferSize = 8;

    private boolean lowLatency = false;

    public DefaultSerialSessionConfig() {
        // All default properties were configured above.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(IoSessionConfig config) {
        super.setAll(config);
        
        if (config instanceof SerialSessionConfig) {
            SerialSessionConfig cfg = (SerialSessionConfig) config;
            setInputBufferSize(cfg.getInputBufferSize());
            setReceiveThreshold(cfg.getReceiveThreshold());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInputBufferSize() {
        return inputBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLowLatency() {
        return lowLatency;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputBufferSize(int bufferSize) {
        inputBufferSize = bufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLowLatency(boolean lowLatency) {
        this.lowLatency = lowLatency;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getReceiveThreshold() {
        return receiveThreshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReceiveThreshold(int bytes) {
        receiveThreshold = bytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOutputBufferSize() {
        return outputBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutputBufferSize(int bufferSize) {
        outputBufferSize = bufferSize;

    }
}
