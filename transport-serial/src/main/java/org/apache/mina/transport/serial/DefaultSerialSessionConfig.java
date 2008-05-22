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

import org.apache.mina.common.AbstractIoSessionConfig;
import org.apache.mina.common.IoSessionConfig;

/**
 * The default configuration for a serial session {@link SerialSessionConfig}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 529576 $, $Date: 2007-04-17 14:25:07 +0200 (mar., 17 avr. 2007) $
 */
class DefaultSerialSessionConfig extends AbstractIoSessionConfig implements SerialSessionConfig {

    private int receiveThreshold = -1;
    private int inputBufferSize = 8;
    private boolean lowLatency = false;

    public DefaultSerialSessionConfig() {
        // All default properties were configured above.
    }

    @Override
    protected void doSetAll(IoSessionConfig config) {
        if (config instanceof SerialSessionConfig) {
            SerialSessionConfig cfg = (SerialSessionConfig) config;
            setInputBufferSize(cfg.getInputBufferSize());
            setReceiveThreshold(cfg.getReceiveThreshold());
        }
    }

    public int getInputBufferSize() {
        return inputBufferSize;
    }

    public boolean isLowLatency() {
        return lowLatency;
    }

    public void setInputBufferSize(int bufferSize) {
        inputBufferSize = bufferSize;
    }

    public void setLowLatency(boolean lowLatency) {
        this.lowLatency = lowLatency;
    }

    public int getReceiveThreshold() {
        return receiveThreshold;
    }

    public void setReceiveThreshold(int bytes) {
        receiveThreshold = bytes;
    }
}
