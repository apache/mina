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

import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;

/**
 * Filter implementation which makes it possible to write {@link InputStream}
 * objects directly using {@link IoSession#write(Object)}. When an
 * {@link InputStream} is written to a session this filter will read the bytes
 * from the stream into {@link IoBuffer} objects and write those buffers
 * to the next filter. When end of stream has been reached this filter will
 * call {@link IoFilter.NextFilter#messageSent(IoSession,WriteRequest)} using the original
 * {@link InputStream} written to the session and notifies
 * {@link org.apache.mina.core.future.WriteFuture} on the
 * original {@link org.apache.mina.core.write.WriteRequest}.
 * <p/>
 * This filter will ignore written messages which aren't {@link InputStream}
 * instances. Such messages will be passed to the next filter directly.
 * </p>
 * <p/>
 * NOTE: this filter does not close the stream after all data from stream
 * has been written. The {@link org.apache.mina.core.service.IoHandler} should take
 * care of that in its
 * {@link org.apache.mina.core.service.IoHandler#messageSent(IoSession,Object)}
 * callback.
 * </p>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public class StreamWriteFilter extends AbstractStreamWriteFilter<InputStream> {

    @Override
    protected IoBuffer getNextBuffer(InputStream is) throws IOException {
        byte[] bytes = new byte[getWriteBufferSize()];

        int off = 0;
        int n = 0;
        while (off < bytes.length
                && (n = is.read(bytes, off, bytes.length - off)) != -1) {
            off += n;
        }

        if (n == -1 && off == 0) {
            return null;
        }

        IoBuffer buffer = IoBuffer.wrap(bytes, 0, off);

        return buffer;
    }
    
    @Override
    protected Class<InputStream> getMessageClass() {
        return InputStream.class;
    }

}
