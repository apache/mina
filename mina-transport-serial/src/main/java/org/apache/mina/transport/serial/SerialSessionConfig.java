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

import org.apache.mina.core.session.IoSessionConfig;

/**
 * An {@link IoSessionConfig} for serial transport type.
 * All those parameters are extracted from rxtx.org API for more details :
 * http://www.rxtx.org
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface SerialSessionConfig extends IoSessionConfig {

    /**
     * Gets the input buffer size. Note that this method is advisory and the underlying OS
     * may choose not to report correct values for the buffer size.
     * @return input buffer size in bytes
     */
    int getInputBufferSize();

    /**
     * Sets the input buffer size. Note that this is advisory and memory availability may
     * determine the ultimate buffer size used by the driver.
     * @param bufferSize the buffer size in bytes
     */
    void setInputBufferSize(int bufferSize);


    /**
     * Gets the output buffer size. Note that this method is advisory and the underlying OS
     * may choose not to report correct values for the buffer size.
     * @return input buffer size in bytes
     */
    int getOutputBufferSize();

    /**
     * Sets the output buffer size. Note that this is advisory and memory availability may
     * determine the ultimate buffer size used by the driver.
     * @param bufferSize the buffer size in bytes
     */
    void setOutputBufferSize(int bufferSize);

    /**
     * Is the low latency mode is enabled.
     * @return low latency on
     */
    boolean isLowLatency();

    /**
     * Set the low latency mode, be carefull it's not supported by all the OS/hardware.
     * @param lowLatency
     */
    void setLowLatency(boolean lowLatency);

    /**
     * The current receive threshold (-1 if not enabled). Give the value of the current buffer
     * needed for generate a new frame.
     * @return the receive thresold in bytes or -1 if disabled
     */
    int getReceiveThreshold();

    /**
     * Set the receive threshold in byte (set it to -1 for disable). The serial port will try to
     * provide frame of the given minimal byte count. Be carefull some devices doesn't support it.
     * @param bytes minimal amount of byte before producing a new frame, or -1 if disabled
     */
    void setReceiveThreshold(int bytes);
    
    
   

}
