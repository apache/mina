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
package org.apache.mina.filter.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;

/**
 * A {@link ProtocolDecoder} that cumulates the content of received
 * buffers to a <em>cumulative buffer</em> to help users implement decoders.
 * <p>
 * If the received {@link IoBuffer} is only a part of a message.
 * decoders should cumulate received buffers to make a message complete or
 * to postpone decoding until more buffers arrive.
 * <p>
 * Here is an example decoder that decodes CRLF terminated lines into
 * <code>Command</code> objects:
 * <pre>
 * public class CrLfTerminatedCommandLineDecoder
 *         extends CumulativeProtocolDecoder {
 *
 *     private Command parseCommand(IoBuffer in) {
 *         // Convert the bytes in the specified buffer to a
 *         // Command object.
 *         ...
 *     }
 *
 *     protected boolean doDecode(
 *             IoSession session, IoBuffer in, ProtocolDecoderOutput out)
 *             throws Exception {
 *
 *         // Remember the initial position.
 *         int start = in.position();
 *
 *         // Now find the first CRLF in the buffer.
 *         byte previous = 0;
 *         while (in.hasRemaining()) {
 *             byte current = in.get();
 *
 *             if (previous == '\r' && current == '\n') {
 *                 // Remember the current position and limit.
 *                 int position = in.position();
 *                 int limit = in.limit();
 *                 try {
 *                     in.position(start);
 *                     in.limit(position);
 *                     // The bytes between in.position() and in.limit()
 *                     // now contain a full CRLF terminated line.
 *                     out.write(parseCommand(in.slice()));
 *                 } finally {
 *                     // Set the position to point right after the
 *                     // detected line and set the limit to the old
 *                     // one.
 *                     in.position(position);
 *                     in.limit(limit);
 *                 }
 *                 // Decoded one line; CumulativeProtocolDecoder will
 *                 // call me again until I return false. So just
 *                 // return true until there are no more lines in the
 *                 // buffer.
 *                 return true;
 *             }
 *
 *             previous = current;
 *         }
 *
 *         // Could not find CRLF in the buffer. Reset the initial
 *         // position to the one we recorded above.
 *         in.position(start);
 *
 *         return false;
 *     }
 * }
 * </pre>
 * <p>
 * Please note that this decoder simply forward the call to
 * {@link #doDecode(IoSession, IoBuffer, ProtocolDecoderOutput)} if the
 * underlying transport doesn't have a packet fragmentation.  Whether the
 * transport has fragmentation or not is determined by querying
 * {@link TransportMetadata}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class CumulativeProtocolDecoder extends ProtocolDecoderAdapter {

    private final AttributeKey BUFFER = new AttributeKey(getClass(), "buffer");

    /**
     * Creates a new instance.
     */
    protected CumulativeProtocolDecoder() {
        // Do nothing
    }

    /**
     * Cumulates content of <tt>in</tt> into internal buffer and forwards
     * decoding request to {@link #doDecode(IoSession, IoBuffer, ProtocolDecoderOutput)}.
     * <tt>doDecode()</tt> is invoked repeatedly until it returns <tt>false</tt>
     * and the cumulative buffer is compacted after decoding ends.
     *
     * @throws IllegalStateException if your <tt>doDecode()</tt> returned
     *                               <tt>true</tt> not consuming the cumulative buffer.
     */
    public void decode(IoSession session, IoBuffer in,
            ProtocolDecoderOutput out) throws Exception {
        if (!session.getTransportMetadata().hasFragmentation()) {
            while (in.hasRemaining()) {
                if (!doDecode(session, in, out)) {
                    break;
                }
            }

            return;
        }

        boolean usingSessionBuffer = true;
        IoBuffer buf = (IoBuffer) session.getAttribute(BUFFER);
        // If we have a session buffer, append data to that; otherwise
        // use the buffer read from the network directly.
        if (buf != null) {
            boolean appended = false;
            // Make sure that the buffer is auto-expanded.
            if (buf.isAutoExpand()) {
                try {
                    buf.put(in);
                    appended = true;
                } catch (IllegalStateException e) {
                    // A user called derivation method (e.g. slice()),
                    // which disables auto-expansion of the parent buffer.
                } catch (IndexOutOfBoundsException e) {
                    // A user disabled auto-expansion.
                }
            }

            if (appended) {
                buf.flip();
            } else {
                // Reallocate the buffer if append operation failed due to
                // derivation or disabled auto-expansion.
                buf.flip();
                IoBuffer newBuf = IoBuffer.allocate(
                        buf.remaining() + in.remaining()).setAutoExpand(true);
                newBuf.order(buf.order());
                newBuf.put(buf);
                newBuf.put(in);
                newBuf.flip();
                buf = newBuf;

                // Update the session attribute.
                session.setAttribute(BUFFER, buf);
            }
        } else {
            buf = in;
            usingSessionBuffer = false;
        }

        for (;;) {
            int oldPos = buf.position();
            boolean decoded = doDecode(session, buf, out);
            if (decoded) {
                if (buf.position() == oldPos) {
                    throw new IllegalStateException(
                            "doDecode() can't return true when buffer is not consumed.");
                }

                if (!buf.hasRemaining()) {
                    break;
                }
            } else {
                break;
            }
        }

        // if there is any data left that cannot be decoded, we store
        // it in a buffer in the session and next time this decoder is
        // invoked the session buffer gets appended to
        if (buf.hasRemaining()) {
            if (usingSessionBuffer && buf.isAutoExpand()) {
                buf.compact();
            } else {
                storeRemainingInSession(buf, session);
            }
        } else {
            if (usingSessionBuffer) {
                removeSessionBuffer(session);
            }
        }
    }

    /**
     * Implement this method to consume the specified cumulative buffer and
     * decode its content into message(s).
     *
     * @param in the cumulative buffer
     * @return <tt>true</tt> if and only if there's more to decode in the buffer
     *         and you want to have <tt>doDecode</tt> method invoked again.
     *         Return <tt>false</tt> if remaining data is not enough to decode,
     *         then this method will be invoked again when more data is cumulated.
     * @throws Exception if cannot decode <tt>in</tt>.
     */
    protected abstract boolean doDecode(IoSession session, IoBuffer in,
            ProtocolDecoderOutput out) throws Exception;

    /**
     * Releases the cumulative buffer used by the specified <tt>session</tt>.
     * Please don't forget to call <tt>super.dispose( session )</tt> when
     * you override this method.
     */
    @Override
    public void dispose(IoSession session) throws Exception {
        removeSessionBuffer(session);
    }

    private void removeSessionBuffer(IoSession session) {
        session.removeAttribute(BUFFER);
    }

    private void storeRemainingInSession(IoBuffer buf, IoSession session) {
        final IoBuffer remainingBuf = IoBuffer.allocate(buf.capacity()).setAutoExpand(true);

        remainingBuf.order(buf.order());
        remainingBuf.put(buf);

        session.setAttribute(BUFFER, remainingBuf);
    }
}
