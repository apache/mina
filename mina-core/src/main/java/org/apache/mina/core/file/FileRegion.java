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
package org.apache.mina.core.file;

import java.nio.channels.FileChannel;

/**
 * Indicates the region of a file to be sent to the remote host.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface FileRegion {

    /**
     * The open <tt>FileChannel</tt> from which data will be read to send to
     * remote host.
     *
     * @return  An open <tt>FileChannel</tt>.
     */
    FileChannel getFileChannel();

    /**
     * The current file position from which data will be read.
     *
     * @return  The current file position.
     */
    long getPosition();

    /**
     * Updates the current file position based on the specified amount. This
     * increases the value returned by {@link #getPosition()} and
     * {@link #getWrittenBytes()} by the given amount and decreases the value
     * returned by {@link #getRemainingBytes()} by the given {@code amount}.
     * 
     * @param amount The new value for the file position.
     */
    void update(long amount);

    /**
     * The number of bytes remaining to be written from the file to the remote
     * host.
     *
     * @return  The number of bytes remaining to be written.
     */
    long getRemainingBytes();

    /**
     * The total number of bytes already written.
     *
     * @return  The total number of bytes already written.
     */
    long getWrittenBytes();

    /**
     * Provides an absolute filename for the underlying FileChannel.
     * 
     * @return  the absolute filename, or <tt>null</tt> if the FileRegion
     *   does not know the filename
     */
    String getFilename();
}
