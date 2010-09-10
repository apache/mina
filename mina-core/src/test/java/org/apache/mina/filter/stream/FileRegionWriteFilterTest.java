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
package org.apache.mina.filter.stream;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.mina.core.file.DefaultFileRegion;
import org.apache.mina.core.file.FileRegion;

/**
 * Tests {@link StreamWriteFilter}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class FileRegionWriteFilterTest extends AbstractStreamWriteFilterTest<FileRegion, FileRegionWriteFilter> {

    @Override
    protected FileRegionWriteFilter createFilter() {
        return new FileRegionWriteFilter();
    }
    
    @Override
    protected FileRegion createMessage(byte[] data) throws IOException {
        File file = File.createTempFile("mina", "unittest");
        file.deleteOnExit();
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        channel.write(buffer);
        return new DefaultFileRegion(channel);
    }

}
